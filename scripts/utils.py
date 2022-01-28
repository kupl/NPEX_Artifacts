import json
import shutil
import time
import os, sys, glob
import re
import subprocess
import signal
from subprocess import Popen, PIPE, TimeoutExpired
from multiprocessing import Pool
from time import monotonic as timer
from copy import deepcopy
from typing import List, Set, Dict, Tuple, Optional, Any
import xml.etree.ElementTree as ET
from config import *
from dacite import from_dict as _from_dict
import pretty_csv
import csv

ENV = os.environ

if os.path.isdir("logs") is False:
    os.mkdir("logs")

logfile = open("logs/execute_log_%s.log" % str(time.strftime("%m%d_%I%M", time.localtime())), "w")


def from_dict(klass, d):
    if d == None:
        return None
    elif d == []:
        return []
    else:
        return _from_dict(data_class=klass, data=d)


def has_field_and_true(dict, field):
    field in dict and dict[field] is True


def find_file(glob_pattern):
    found = []
    for file in glob.glob(glob_pattern, recursive=True):
        found.append(file)

    if len(found) > 1:
        return found
    elif len(found) == 1:
        return found
    else:
        raise FileNotFoundError()


def size_of(repo_dir):
    cmd = f"cloc . --match-f=.java --json --exclude-dir=infer-out --exclude-dir=patches --exclude-dir=captured --exclude-dir=patches --exclude-dir=infer-out-cached"
    # print(cmd)
    ret_cloc = execute(cmd, repo_dir)
    if ret_cloc.stdout == "":
        print(f"[WARNING]: {repo_dir} is not measured by cloc")
        return None
    data = json.loads(ret_cloc.stdout)
    return data["Java"]["code"]


def parse_error(error_message):
    if MSG_COMPILE_FAIL in error_message:
        return MSG_COMPILE_FAIL
    elif MSG_ASSERT_FAIL in error_message:
        return MSG_ASSERT_FAIL
    elif MSG_NPE in error_message:
        return MSG_NPE
    elif MSG_TEST_FAIL in error_message:
        return MSG_TEST_FAIL
    else:
        return "UNKNOWN_ERROR"


def find_java_version(poms):
    for pom in poms:
        print(f" - parsing {pom}")
        root = ET.parse(pom).getroot()
        nsmap = {"m": root.tag.rstrip("project").lstrip("{").rstrip("}")}
        if root.find("m:properties/m:java.src.version", nsmap):
            return root.find("m:properties/m:java.src.version", nsmap).text.split(".")[-1]  # 1.7, 1.8
        else:
            plugins = root.findall("m:build/m:plugins/m:plugin", nsmap) + root.findall(
                "m:build/m:pluginManagement/m:plugins/m:plugin", nsmap)
            for plugin in plugins:
                art = plugin.find("m:artifactId", nsmap)
                if "compiler" in art.text and plugin.find("m:configuration/m:source", nsmap):
                    return plugin.find("m:configuration/m:source", nsmap).text.split(".")[-1]

            # Not found
            jdk = root.find("m:profiles/m:profile/m:activation/m:jdk", nsmap)
            if jdk is None:
                continue  # default
            else:
                return jdk.text[1:].split(",")[0].split(".")[-1]
    return None

def get_compile_command(
    cwd,
    project=None,
    java_version=None,
    phase="test-compile",
    mvn_additional_options=[],
):
    # skip_tests = "-DskipTests"
    if os.path.isfile(f"{cwd}/pom.xml"):
        return f"mvn clean {phase} {MVN_OPTION} {' '.join(mvn_additional_options)}"
    elif os.path.isfile(f"{cwd}/main.java"):
        return "javac main.java"  # for test
    elif os.path.isfile(f"{cwd}/gradlew"):
        return "./gradlew assemble"
    elif os.path.isfile(f"{cwd}/build.xml"):
        return "ant compile"
    else:
        return None


def remove_terminal(str):
    ansi_escape = re.compile(r"\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])")
    return ansi_escape.sub("", str)


class Ret:
    def __init__(self, stdout, stderr, return_code, time):
        self.stdout = stdout.decode()
        self.stderr = stderr.decode()
        self.return_code = return_code
        self.time = time


def execute(cmd, dir=None, env=None, timeout=1200, verbosity=0):
    if dir == None:
        print(f"{ERROR}: dir is not given while executing {cmd}")
        exit(1)

    if verbosity >= 1:
        print(f"EXECUTE {cmd} AT {os.path.basename(dir)}")

    start = timer()
    try:
        process = Popen(
            cmd,
            shell=True,
            stdout=PIPE,
            stderr=PIPE,
            cwd=dir,
            env=env,
            preexec_fn=os.setsid,
        )
        stdout, stderr = process.communicate(timeout=timeout)
        returncode = process.returncode
    except TimeoutExpired:
        os.killpg(process.pid, signal.SIGINT)  # send signal to the process group
        print(f"{TIMEOUT} occurs during executing {cmd[:20]} at {dir}")
        stdout, stderr = b"", b""
        returncode = -1
    except OSError:
        print(f"{ERROR}: failed to execute {cmd} at {dir} (maybe it is too long...)")
        stdout, stderr = b"", b""
        returncode = -1

    ret = Ret(stdout, stderr, returncode, timer() - start)

    err_msg = ("=== Execute %s ===\n  * return_code : %d\n  * stdout : %s\n  * stderr : %s\n  * dir : %s\n" %
               (cmd, ret.return_code, ret.stdout, ret.stderr, dir))
    if ret.return_code != 0:
        if verbosity >= 1:
            print(f"{ERROR} - FAILED TO EXECUTE {cmd} AT {os.path.basename(dir)}")
        logfile.write(err_msg)
        logfile.flush()
    return ret


def get_test_command(dir, test_classes=[], project=None, java_version=8):
    if os.path.isfile(f"{dir}/pom.xml"):
        project_str = f"-pl {project}" if project else ""
        test_classes = ",".join(test_classes)
        test_str = f"-Dtest={test_classes} -DfailIfNoTests=false"
        return f"mvn clean test -fn {project_str} {test_str} {MVN_OPTION}"
    elif os.path.isfile(f"{dir}/build.xml"):
        return 'ant test -logfile "results.txt"'
    elif os.path.isfile(f"{dir}/gradlew"):  # build.gradle
        return "./gradlew test"
    else:
        print(f" - {os.path.basename(dir)} has no pom.xml, build.xml, gradlew")
        return None


def read_csv_from_file(csv_filename: str):
    with open(csv_filename, 'r') as f:
        reader = csv.DictReader(f)
        return [row for row in reader]


def is_empty_or_no_json(filepath):
    if os.path.isfile(filepath) is False:
        return True
    json_file = open(filepath, "r")
    json_str = json_file.read()
    if json_str == "":
        return True
    elif json_str == "[]":
        return True
    else:
        return False


def remove_if_exists(filepath):
    basedir = os.path.split(filepath)[0]
    if os.path.isfile(filepath):
        execute(f"rm {filepath}", dir=basedir)
    elif os.path.isdir(filepath):
        shutil.rmtree(filepath)


def read_json_from_file(json_filename: str):
    json_file = open(json_filename, "r")
    json_str = json_file.read()
    if json_str == "":
        return {}
    else:
        return json.loads(json_str)


def save_dict_to_jsonfile(json_filename: str, dict):
    json_file = open(json_filename, "w")
    json_file.write(json.dumps(dict, indent=4))


def save_dict_to_csvfile(csv_filename: str, dicts: List[Dict]):
    with open(csv_filename, 'w') as f:
        w = csv.DictWriter(f, dicts[0].keys())
        w.writeheader()
        for target_row in dicts:
            w.writerow(target_row)


def multiprocess(fun, arg_list, n_cpus=4):
    with Pool(n_cpus) as p:
        return p.map(fun, arg_list)


def copyfile(src, dst, inner=False, verbosity=0):
    if os.path.isdir(src) and inner:
        execute(f"cp -r {src}/* {dst}", dir=os.getcwd(), verbosity=verbosity)
    else:
        execute(f"cp -r {src} {dst}", dir=os.getcwd(), verbosity=verbosity)


def pretty_print_dict_to_csv(output, dicts: List[Dict[str, Any]]):
    tmp_csv = f"{output}.csv"
    save_dict_to_csvfile(tmp_csv, dicts)
    pretty_csv.pretty_file(tmp_csv, new_filename=output)
    
    
def pretty_print_dict_to_md(output, dicts: List[Dict[str, Any]]):
    tmp_csv = f"{output}.csv"
    save_dict_to_csvfile(tmp_csv, dicts)
    pretty_csv.csv_to_markdown(tmp_csv, output)
    