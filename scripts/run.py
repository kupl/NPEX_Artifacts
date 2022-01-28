#!/usr/bin/python3.8
from __future__ import annotations
import argparse
import time
import glob
import os
import utils
from pprint import pprint
from typing import List, Any, Dict, Tuple
from dacite import from_dict as _from_dict
from dataclasses import asdict, dataclass, field, fields, is_dataclass
from config import *
from eval_config import *
from functools import partial
from data import *

def clean(target: Target):
    utils.execute(f"git clean -df .", dir=target.bug_dir)
    utils.execute(f"git checkout -f .", dir=target.bug_dir)
    if target.is_vfix is False:
        utils.execute(f"mvn clean", dir=target.bug_dir)



def check_build(target: Target):
    utils.remove_if_exists(f"{target.bug_dir}/infer-out-cached")
    utils.remove_if_exists(f"{target.bug_dir}/infer-out-reduced")

    utils.execute(f"{NPEX_analyzer_script} --capture", dir=target.bug_dir)
    is_captured = utils.execute(f"{INFER} npex", dir=target.bug_dir).return_code == 0

    utils.execute(f"{NPEX_CMD} build {target.bug_dir}", dir=target.bug_dir)
    is_spoon_build = os.path.isfile(f"{target.bug_dir}/.spoon-model.cache")

    if is_captured and is_spoon_build:
        print(f"{SUCCESS} to build {target.bug_id}")
        return True
    elif is_captured:
        print(f"{FAIL} to build {target.bug_id} by Spoon")
        return False
    elif is_spoon_build:
        print(f"{FAIL} to build {target.bug_id} by Infer")
        return False
    else:
        print(f"{FAIL} to build {target.bug_id} by Infer & Spoon")
        return False


def localize(target: Target):
    bug_dir = target.bug_dir
    bug_id = target.bug_id
    TIMEOUT = 3600
    for report in glob.glob(f"{bug_dir}/npe*.json"):
        if os.path.basename(report) != "npe.json":
            utils.execute(f"rm {report}", dir=bug_dir)

    utils.remove_if_exists(f"{bug_dir}/.timeout_localization")
    utils.remove_if_exists(f"{bug_dir}/localizer_result.json")

    ret = utils.execute(f"python3.8 {NPEX_analyzer_script} --localize ", dir=bug_dir, timeout=TIMEOUT)

    if ret.time > TIMEOUT:
        utils.execute(f"touch {bug_dir}/.timeout_localization", dir=bug_dir)

    reports = glob.glob(f"{bug_dir}/npe*.json")
    return os.path.isfile(f"{bug_dir}/localizer_result.json")


def generate_patch(target: Target):
    bug_dir = target.bug_dir
    bug_id = target.bug_id
    target.checkout()
    reports = glob.glob(f"{bug_dir}/npe*.json")
    if os.path.isdir(f"{bug_dir}/patches"):
        utils.execute(f"rm -rf {bug_dir}/patches", dir=bug_dir)

    if os.path.isfile(f"{bug_dir}/.spoon-model.cache") is False:
        utils.execute(f"{NPEX_CMD} build {target.bug_dir}", dir=target.bug_dir)

    for report in reports:
        utils.execute(f"{NPEX_CMD} patch {bug_dir} --report={report} --cached", dir=bug_dir)

    patches = glob.glob(f"{bug_dir}/patches/*/patch.java")
    if patches == []:
        return False
    else:
        return True


def predict(target: Target):
    bug_dir = target.bug_dir
    classifiers = f"{CLASSIFIER_DIR}/{target.bug_id}.classifier"
    utils.remove_if_exists(f"{bug_dir}/model.json")

    target.checkout()
    utils.execute( f"python3.8 {NPEX_analyzer_script} --predict --classifiers {classifiers}", dir=bug_dir)
    if utils.is_empty_or_no_json(f"{bug_dir}/model.json"):
        utils.execute(f"rm {bug_dir}/model.json", dir=bug_dir)
        return False
    else:
        return True


def validate(target: Target):
    bug_dir = target.bug_dir

    if os.path.isfile(f"{bug_dir}/learned_result.json"):
        utils.execute(f"rm {bug_dir}/learned_result.json", dir=bug_dir)

    utils.remove_if_exists(f"{bug_dir}/.timeout")
    utils.remove_if_exists(f"{bug_dir}/result.json")
    TIMEOUT = 3600
    ret = utils.execute( f"python3.8 {NPEX_analyzer_script}", dir=bug_dir, timeout=TIMEOUT)

    if ret.time > TIMEOUT:
        utils.execute(f"touch {bug_dir}/.timeout", dir=bug_dir)
        return False
    
    return os.path.isfile(f"{bug_dir}/result.json")


def run_target(target: Target):
    TIMEOUT = 3600.0
    start_time = time.time()
    print(f"{PROGRESS}: running {target.bug_id}")
    if localize(target) is False:
        print(f"{FAIL}: to localize {target.bug_id}")
        return

    print(f" - localization is done...")
    if generate_patch(target) is False:
        print(f"{FAIL}: to generate patch {target.bug_id}")
        return

    print(f" - patch candidates is generated...")
    if predict(target) is False:
        print(f"{FAIL}: to predict null handling model for {target.bug_id}")
        return

    print(f" - null handling model is instantiated...")
    if validate(target) is False:
        print(f"{FAIL}: to infer meaningful specification for {target.bug_id}")
        return
    elapsed_time = time.time() - start_time

    labels = Label.labels_from_json(f"{LABEL_DIR}/{target.bug_id}.json", target)
    result = Result.from_target(target, list(labels.values()))
    if elapsed_time > TIMEOUT:
        print(f"{FAIL}: timed out for {target.bug_id}")
    else:
        print(f"{SUCCESS}: to infer spec & validate patches for {target.bug_id}, {elapsed_time}, {result.result}")
    return


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--genesis_rg", default=False, action='store_true', help="run genesis runnable regression")
    parser.add_argument('--cpus', default=10, type=int, help='number of cpus to use')
    subparser = parser.add_subparsers(dest='subcommand')
    prepare_args = subparser.add_parser('prepare', help='prepare evaluation')
    prepare_args.add_argument('--clean', action='store_true', help='clean intermediate results')
    validate_args = subparser.add_parser('run', help='run NPEX')
    evaluate_args = subparser.add_parser('evaluate', help='evaluate results')

    args = parser.parse_args()
    targets = Target.targets_from_json_file(f"{BENCH_DIR}/targets.json")

    for target in targets:
        target.bug_dir = f"{BENCH_DIR}/{target.bug_dir}"
        target.root_dir = f"{BENCH_DIR}/{target.root_dir}"

    if args.subcommand == 'prepare':
        if args.clean:
            utils.multiprocess(clean, targets, n_cpus=args.cpus)
        else:
            utils.multiprocess(check_build, targets, n_cpus=args.cpus)

    elif args.subcommand == 'run':
        for target in targets:
            run_target(target)

    elif args.subcommand == 'evaluate':
        results = []
        for target in targets:
            labels = Label.labels_from_json(
                f"{LABEL_DIR}/{target.bug_id}.json", target)
            result = Result.from_target(target, list(labels.values())).asdict()
            results.append(result)
        results.sort(key=lambda x: (x["source"], x['bug_id'], x['result']))
        utils.pretty_print_dict_to_csv("evaluate.results", results)

