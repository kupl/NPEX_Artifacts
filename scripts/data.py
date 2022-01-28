#!/usr/bin/python3.8
from __future__ import annotations
import os
import utils
import glob
import time
from config import *
from eval_config import *
from typing import List, Any, Dict, Optional
from dataclasses import asdict, dataclass, field, fields, is_dataclass
from enum import Enum
import random
from pprint import pprint


@dataclass(unsafe_hash=True)
class Patch:
    contents: str
    original_filepath: str
    patched_line: int
    # could be changed, so it is not used for compare
    strategy: str = field(hash=None, compare=False)
    # could be changed, so it is not used for compare
    patch_id: str = field(hash=None, compare=False)

    @staticmethod
    def from_dict(patch_dict) -> Patch:
        patch_id = os.path.basename(patch_dict['patch_id'])
        strategy = patch_dict['strategy']
        contents = patch_dict['contents']
        original_filepath = patch_dict['original_filepath']
        patched_line = patch_dict['patched_lines'][
            0] if 'patched_lines' in patch_dict else patch_dict['patched_line']
        return Patch(contents=contents,
                     original_filepath=original_filepath,
                     patched_line=patched_line,
                     strategy=strategy,
                     patch_id=patch_id)

    @staticmethod
    def from_json(patch_json) -> Patch:
        patch_dict = utils.read_json_from_file(patch_json)
        patch_id = os.path.basename(
            os.path.split(patch_json)[0])  # .../{patch_id}/patch.json
        strategy = os.path.basename(patch_id).split('_')[
            0]  # patch_id = strategy_id
        contents = patch_dict['contents']
        original_filepath = patch_dict['original_filepath']
        patched_line = patch_dict['patched_lines'][
            0] if 'patched_lines' in patch_dict else patch_dict['patched_line']
        return Patch(contents=contents,
                     original_filepath=original_filepath,
                     patched_line=patched_line,
                     strategy=strategy,
                     patch_id=patch_id)


@dataclass
class RawResult:
    number_of_patches: int
    verified_patches: List[str]
    rejected_patches: List[str]
    time_to_verify: float
    time_to_inference: float
    time_to_capture_original: float
    time_to_capture_patches: float
    pass_label: bool = True

    @classmethod
    def from_json(cls, jsonfile):
        if os.path.isfile(jsonfile):
            result_json = utils.read_json_from_file(jsonfile)
            return utils.from_dict(cls, result_json)
        else:
            return RawResult(0, [], [], 0.0, 0.0, 0.0, 0.0, False)

    def to_json(self, bug_dir):
        utils.save_dict_to_jsonfile(f"{bug_dir}/result.json", asdict(self))


class ResultOverall(Enum):
    WEAK_CORRECT = 2
    CORRECT = 1
    NO_CORRECT = 0
    OVERFITTING = -1
    FAIL = -2
    TODO = -3

    def to_string(self):
        if self is ResultOverall.WEAK_CORRECT:
            return "INCORRECT"
            return "WEAK_CORRECT"
        elif self is ResultOverall.CORRECT:
            return "CORRECT"
        elif self is ResultOverall.NO_CORRECT:
            return "NO_PATCH"
        elif self is ResultOverall.OVERFITTING:
            return "INCORRECT"
        elif self is ResultOverall.FAIL:
            return "NO_PATCH"
        elif self is ResultOverall.TODO:
            return "TODO"


class ResultDetail(Enum):
    VERIFY_CORRECT = 1
    VERIFY_OVERFIT = -1
    REJECT_OVERFIT = 2
    REJECT_CORRECT = -2
    NO_PATCH = -3
    NO_SPEC = -4
    NO_INVO_CONTEXT = -5
    FAIL_SPOON = -6
    TO_VERIFICATION = -7
    TO_LOCALIZATION = -8
    FAIL_LOCAL = -9

    def to_string(self):
        if self is ResultDetail.VERIFY_CORRECT:
            return "VERIFY_CORRECT"
        elif self is ResultDetail.VERIFY_OVERFIT:
            return "VERIFY_OVERFIT"
        elif self is ResultDetail.REJECT_OVERFIT:
            return "REJECT_OVERFIT"
        elif self is ResultDetail.REJECT_CORRECT:
            return "REJECT_CORRECT"
        elif self is ResultDetail.NO_PATCH:
            return "NO_PATCH"
        elif self is ResultDetail.NO_SPEC:
            return "NO_SPEC"
        elif self is ResultDetail.NO_INVO_CONTEXT:
            return "NO_INVO_CONTEXT"
        elif self is ResultDetail.FAIL_SPOON:
            return "FAIL_SPOON"
        elif self is ResultDetail.TO_VERIFICATION:
            return "TIMEOUT_VERI"
        elif self is ResultDetail.TO_LOCALIZATION:
            return "TIMEOUT_LOCA"
        elif self is ResultDetail.FAIL_LOCAL:
            return "FAIL_LOCAL"


class LabelOverall(Enum):
    WC = 2
    CORRECT = 1
    OVERFITTING = 0
    AMBIGUOUS = -1
    TODO = -2
    EXCLUDED = -3
    PASSTEST = -4
    TIMEOUT = -5

    @staticmethod
    def from_string(str):
        if "CORRECT" in str:
            return LabelOverall.CORRECT
        elif "WC" in str:
            return LabelOverall.WC
        elif "OVERFITTING" in str:
            return LabelOverall.OVERFITTING
        elif "AMBIGUOUS" in str:
            return LabelOverall.AMBIGUOUS
        elif "TODO" in str:
            return LabelOverall.TODO
        elif "EXCLUDED" in str:
            return LabelOverall.EXCLUDED
        elif "PASSTEST" in str:
            return LabelOverall.PASSTEST
        elif "TIMEOUT" in str:
            return LabelOverall.TIMEOUT

    def to_string(self):
        if self is LabelOverall.CORRECT:
            return "LabelOverall.CORRECT"
        elif self is LabelOverall.WC:
            return "LabelOverall.WC"
        elif self is LabelOverall.OVERFITTING:
            return "LabelOverall.OVERFITTING"
        elif self is LabelOverall.AMBIGUOUS:
            return "LabelOverall.AMBIGUOUS"
        elif self is LabelOverall.TODO:
            return "LabelOverall.TODO"
        elif self is LabelOverall.EXCLUDED:
            return "LabelOverall.EXCLUDED"
        elif self is LabelOverall.PASSTEST:
            return "LabelOverall.PASSTEST"
        elif self is LabelOverall.TIMEOUT:
            return "LabelOverall.TIMEOUT"

    @staticmethod
    def require_manual(label):
        return label is LabelOverall.TODO or label is LabelOverall.PASSTEST or label is LabelOverall.TIMEOUT


class LabelDetail(Enum):
    MANUAL_WC = 3
    SYNEQUAL = 2
    MANUAL_CORRECT = 1
    MANUAL_INCORRECT = 0
    TEST_FAIL = -1
    COMPILE_FAIL = -2
    TODO = -3
    PASS_TEST = -4
    TIMEOUT = -5
    # TODO: labels for differential testing/analysis

    @staticmethod
    def from_string(str):
        if "SYNEQUAL" in str:
            return LabelDetail.SYNEQUAL
        elif "MANUAL_WC" in str:
            return LabelDetail.MANUAL_WC
        elif "MANUAL_CORRECT" in str:
            return LabelDetail.MANUAL_CORRECT
        elif "MANUAL_INCORRECT" in str:
            return LabelDetail.MANUAL_INCORRECT
        elif "TEST_FAIL" in str:
            return LabelDetail.TEST_FAIL
        elif "COMPILE_FAIL" in str:
            return LabelDetail.COMPILE_FAIL
        elif "TODO" in str:
            return LabelDetail.TODO
        elif "PASS_TEST" in str:
            return LabelDetail.PASS_TEST
        elif "TIMEOUT" in str:
            return LabelDetail.TIMEOUT

    def to_string(self):
        if self is LabelDetail.SYNEQUAL:
            return "LabelDetail.SYNEQUAL"
        elif self is LabelDetail.MANUAL_WC:
            return "LabelDetail.MANUAL_WC"
        elif self is LabelDetail.MANUAL_CORRECT:
            return "LabelDetail.MANUAL_CORRECT"
        elif self is LabelDetail.MANUAL_INCORRECT:
            return "LabelDetail.MANUAL_INCORRECT"
        elif self is LabelDetail.TEST_FAIL:
            return "LabelDetail.TEST_FAIL"
        elif self is LabelDetail.COMPILE_FAIL:
            return "LabelDetail.COMPILE_FAIL"
        elif self is LabelDetail.TODO:
            return "LabelDetail.TODO"
        elif self is LabelDetail.PASS_TEST:
            return "LabelDetail.PASS_TEST"
        elif self is LabelDetail.TIMEOUT:
            return "LabelDetail.TIMEOUT"


@dataclass
class Label:
    bug_id: str
    patch: Patch
    label: LabelOverall
    detail: LabelDetail
    description: Optional[str] = None

    def is_done(self) -> bool:
        if LabelOverall.require_manual(self.label):
            print(f"{self.label} require manual labeling")
            return False
        else:
            print(f"{self.label} does not require manual labeling")
            return True

    def is_correct(self) -> bool:
        return self.label is LabelOverall.CORRECT

    def asdict(self) -> Dict[str, Any]:
        return {
            "bug_id": self.bug_id,
            "patch": asdict(self.patch),
            "label": self.label.to_string(),
            "detail":
            self.detail.to_string() if self.detail else "LabelDetail.TODO",
            "description": self.description
        }

    @staticmethod
    def labels_from_json(filepath: str, target: Target) -> Dict[str, Label]:
        labels: List[Label] = []
        bug_id = target.bug_id
        if os.path.isfile(filepath):
            for label_dict in utils.read_json_from_file(filepath):
                patch = Patch.from_dict(label_dict['patch'])
                label = LabelOverall.from_string(label_dict['label'])
                detail = LabelDetail.from_string(label_dict['detail'])
                description = label_dict[
                    'description'] if 'description' in label_dict else None
                labels.append(Label(bug_id, patch, label, detail, description))

        patch_dirs = [
            patch_dir for patch_dir in glob.glob(f"{target.bug_dir}/patches/*")
            if os.path.isfile(f"{patch_dir}/patch.java")
        ]
        patch_id_map = {}
        for patch_dir in patch_dirs:
            if 'contents' not in utils.read_json_from_file(
                    f"{patch_dir}/patch.json"):
                continue
            patch = Patch.from_json(f"{patch_dir}/patch.json")
            patch_id = patch.patch_id
            patch_id_map[patch] = patch_id

        label_map: Dict[str, Label] = {}
        i = -1
        for label in labels:
            if label.patch not in patch_id_map:
                i -= 1
                label.patch.patch_id = f"{label.patch.patch_id}-{i}"
                label_map[label.patch.patch_id] = label
                continue
            patch_id = patch_id_map[label.patch]
            label.patch.patch_id = patch_id
            label_map[patch_id] = label

        # unlabeled patches
        for patch in patch_id_map.keys():
            patch_id = patch_id_map[patch]
            if patch_id not in label_map:
                # print(f"{patch_id} is not labeled")
                label_map[patch_id] = Label(bug_id, patch, LabelOverall.TODO,
                                            LabelDetail.TODO)

        return label_map

    @staticmethod
    def labels_to_json(labels: Dict[str, Label], output_path: str):
        if labels == {}:
            return

        label_list = list(labels.values())
        label_dicts = [label.asdict() for label in label_list]
        utils.save_dict_to_jsonfile(output_path, label_dicts)

        bug_id = label_list[0].bug_id
        utils.save_dict_to_jsonfile(f"{ROOT_DIR}/labels/{bug_id}.json",
                                    label_dicts)

    @staticmethod
    def pretty_print_labels(labels: Dict[str, Label], output_path: str):
        if labels == {}:
            return

        label_list = list(labels.values())
        bug_id = label_list[0].bug_id

        label_dicts = []
        for label in label_list:
            if '--' in label.patch.patch_id:
                continue
            label_dict = label.asdict()
            del label_dict['patch']
            label_dict['bug_id'] = label.bug_id
            label_dict['patch_id'] = label.patch.patch_id
            label_dicts.append(label_dict)

        if label_dicts == []:
            print(f"{WARNING}: no label in {bug_id}")
            return
        utils.save_dict_to_csvfile(output_path, label_dicts)
        utils.pretty_csv.pretty_file(output_path,
                                     new_filename=f"{output_path}.pretty")

        utils.save_dict_to_csvfile(f"{ROOT_DIR}/labels/{bug_id}.csv",
                                   label_dicts)
        utils.pretty_csv.pretty_file(
            output_path, new_filename=f"{ROOT_DIR}/labels/{bug_id}.pretty")


@dataclass
class Result:
    bug_id: str
    bug_class: str
    kLoc: int
    infer_time: float
    n_patches: int
    verify_time: float
    compile_time: float
    result: ResultOverall
    has_correct: bool
    incorrect_passed: bool

    # manual_result: ResultOverall
    # detail: ResultDetail

    def asdict(self) -> Dict[str, str]:
        # print(self)
        if self.bug_id in vfix_benches:
            source = "VFix"
        elif "Bears" in self.bug_id:
            source = "Bears"
        elif "-buggy" in self.bug_id:
            source = "Genesis"
        else:
            source = "Ours"
        return {
            "bug_id": self.bug_id,
            "source": source,
            # "Loc": str(self.kLoc),
            "bug_class": self.bug_class,
            "result": self.result.to_string(),
            "time_to_infer": f"{self.infer_time:.2f}",
            "#patches": str(self.n_patches),
            "time_to_validate": f"{self.verify_time:.2f}",
            "comp": f"{self.compile_time:.2f}",
            "Exist": str(self.has_correct),
            "TC_fail": str(self.incorrect_passed),
        }

    @staticmethod
    def configure_bug_class(bug_id: str) -> str:
        if bug_id in genesis_reg:
            return "Correct"
        else:
            return "Others"

    @staticmethod
    def configure_result(result_path: str, correct_patches: List[str],
                         incorrect_patches: List[str],
                         weak_correct_patches: List[str]):
        if os.path.isfile(result_path) is False:
            return ResultOverall.FAIL, 0.0, 0.0, 0.0
        raw_result = RawResult.from_json(result_path)

        incorrect_patch_passed = False
        correct_patch_passed = False
        weak_correct_passed = False

        for patch_passed in raw_result.verified_patches:
            if patch_passed in incorrect_patches:
                # print(f"{result_path} has incorrect patch {patch_passed}")
                incorrect_patch_passed = True
            elif patch_passed in correct_patches:
                correct_patch_passed = True
            elif patch_passed in weak_correct_patches:
                weak_correct_passed = True

        compile_time = raw_result.time_to_capture_original + \
            raw_result.time_to_capture_patches
        analysis_time = raw_result.time_to_inference + raw_result.time_to_verify
        result_overall = None
        result_detail = None
        if incorrect_patch_passed is False and correct_patch_passed is True:
            result_overall, result_detail = ResultOverall.CORRECT, ResultDetail.VERIFY_CORRECT
        elif incorrect_patch_passed is False and weak_correct_passed is True:
            result_overall, result_detail = ResultOverall.WEAK_CORRECT, ResultDetail.REJECT_CORRECT
        elif incorrect_patch_passed is False and correct_patches == []:
            # TODO: some may have no correct patches because of bug in patch-generation
            result_overall, result_detail = ResultOverall.NO_CORRECT, ResultDetail.REJECT_OVERFIT
        elif incorrect_patch_passed is False:
            result_overall, result_detail = ResultOverall.NO_CORRECT, ResultDetail.REJECT_CORRECT
        elif correct_patch_passed is True:
            result_overall, result_detail = ResultOverall.OVERFITTING, ResultDetail.VERIFY_OVERFIT
        elif correct_patch_passed is False and correct_patches == []:
            result_overall, result_detail = ResultOverall.OVERFITTING, ResultDetail.VERIFY_OVERFIT
        elif correct_patch_passed is False:
            result_overall, result_detail = ResultOverall.OVERFITTING, ResultDetail.VERIFY_OVERFIT
        else:
            raise Exception(f"weird result")
        infer_time = float(raw_result.time_to_inference)
        verify_time = float(raw_result.time_to_verify)
        return result_overall, infer_time, verify_time, compile_time

    @staticmethod
    def from_target(target, labels: List[Label]) -> Result:
        bug_dir = target.bug_dir
        bug_id = target.bug_id
        bug_class = Result.configure_bug_class(bug_id)
        # print(f"counting size of {target.bug_id}")
        # kLoc = utils.size_of(target.bug_dir)
        kLoc = 0
        infer_time = 0.0
        n_patches = len(glob.glob(f"{target.bug_dir}/patches/*/patch.java"))
        verify_time = 0.0
        compile_time = 0.0

        correct_patches = []
        incorrect_patches = []
        weak_correct_patches = []
        incorrect_passed = False
        for label in labels:
            if label.label is LabelOverall.CORRECT:
                correct_patches.append(label.patch.patch_id)
            elif label.label is LabelOverall.OVERFITTING:
                incorrect_patches.append(label.patch.patch_id)
                if label.detail is not LabelDetail.COMPILE_FAIL and label.detail is not LabelDetail.TEST_FAIL and label.detail is not LabelDetail.MANUAL_CORRECT:
                    # print(f"{label.detail} is not testfail or compile-fail")
                    incorrect_passed = True
            elif label.label is LabelOverall.WC:
                weak_correct_patches.append(label.patch.patch_id)
        has_correct = correct_patches != []

        result = ResultOverall.FAIL
        if os.path.isfile(f"{bug_dir}/.spoon-model.cache") is False:
            return Result(bug_id, bug_class, kLoc, infer_time, n_patches,
                          verify_time, compile_time, result, has_correct,
                          incorrect_passed)

        if glob.glob(f"{bug_dir}/patches/*/patch.java") == []:
            return Result(bug_id, bug_class, kLoc, infer_time, n_patches,
                          verify_time, compile_time, result, has_correct,
                          incorrect_passed)

        if os.path.isfile(f"{bug_dir}/.timeout_localization"):
            return Result(bug_id, bug_class, kLoc, infer_time, n_patches,
                          verify_time, compile_time, result, has_correct,
                          incorrect_passed)

        if os.path.isfile(f"{bug_dir}/localizer_result.json") is False:
            return Result(bug_id, bug_class, kLoc, infer_time, n_patches,
                          verify_time, compile_time, result, has_correct,
                          incorrect_passed)

        if os.path.isfile(f"{bug_dir}/.timeout"):
            return Result(bug_id, bug_class, kLoc, 3600.0, n_patches,
                          verify_time, compile_time, result, has_correct,
                          incorrect_passed)

        if os.path.isfile(f"{bug_dir}/model.json") is False:
            result, detail = ResultOverall.FAIL, ResultDetail.NO_INVO_CONTEXT

        result, infer_time, verify_time, compile_time = Result.configure_result(
            f"{bug_dir}/result.json", correct_patches,
            incorrect_patches, weak_correct_patches)
        return Result(bug_id, bug_class, kLoc, infer_time, n_patches,
                      verify_time, compile_time, result, has_correct,
                      incorrect_passed)


@dataclass
class Target:
    root_dir: str  # for clone
    bug_dir: str
    bug_id: str
    build_cmd: str
    test_cmd: str
    test_method: str
    is_vfix: bool
    is_benchmark: bool
    artifact_id: str

    @staticmethod
    def is_vfix_dir(bug_dir):
        return os.path.basename(bug_dir) == "source"

    @staticmethod
    def get_bug_id(bug_dir):
        if Target.is_vfix_dir(bug_dir):
            return os.path.basename(os.path.split(bug_dir)[0])
        else:
            return os.path.basename(bug_dir)

    @staticmethod
    def get_artifact_id(bug_dir):
        parent, _ = os.path.split(bug_dir)
        if os.path.isfile(f"{parent}/.artifact-id"):
            print(f"find artifact id in {parent}/.artifact-id")
            return open(f"{parent}/.artifact-id").read().strip("\n")
        else:
            tree = ET.parse(f'{bug_dir}/pom.xml')
            group = tree.findtext('{*}groupId')
            id = tree.findtext('{*}artifactId')
            return f'{group}.{id}' if group != None else id
            # print(f"{FAIL}: to find artifact id in {parent}/.artifact-id")
            # cmd = "mvn -q -Dexec.executable=echo -Dexec.args='${project.artifactId}' --non-recursive exec:exec 2>/dev/null"
            # artifact_id = utils.execute(cmd, dir=bug_dir).stdout.strip("\n")
            # open(f"{bug_dir}/.artifact_id", 'w').write(artifact_id)
            # return artifact_id

    @staticmethod
    def from_old_target(target_old: Dict[str, str]) -> Target:
        bug_dir = target_old["bug_dir"]
        bug_id = target_old["bug_id"]
        build_cmd = target_old["build_cmd"]
        test_cmd = target_old["test_cmd"]
        is_benchmark = True
        is_vfix = os.path.basename(bug_dir) == "source"
        print(f"bug_dir: {bug_dir}")
        artifact_id = Target.get_artifact_id(bug_dir)
        if is_vfix:
            test_method = "main"
            root_dir = f"{ROOT_DIR}/VFix"
        else:
            test_method = utils.read_json_from_file(
                f"{bug_dir}/bug.json")["test_info"]["testcases"][0]["method"]
            if os.path.basename(
                    bug_dir) != bug_id and "root_dir" not in target_old:
                print(f"{ERROR}: root-dir is not given for {bug_id}")
                print(target_old)
            elif os.path.basename(bug_dir) != bug_id:
                root_dir = target_old["root_dir"]
            else:
                root_dir = bug_dir
        print(f"artifact_id of {bug_id}: {artifact_id}")
        bug_dir_rel = os.path.relpath(bug_dir, start=ROOT_DIR)
        root_dir_rel = os.path.relpath(root_dir, start=ROOT_DIR)
        return Target(root_dir_rel, bug_dir_rel, bug_id, build_cmd, test_cmd,
                      test_method, is_vfix, is_benchmark, artifact_id)

    @staticmethod
    def target_from_vfix_dir(bug_dir):
        os.chdir(bug_dir)
        jar_path = ':'.join(glob.glob(f"../../deps/*.jar"))
        class_path = f"{jar_path}:.:../target/classes"
        java_files = glob.glob(f"{bug_dir}/**/*.java", recursive=True)
        for java_file in java_files:
            utils.execute(f"javac -cp {class_path} {java_file}", dir=bug_dir)
        java_files_to_compile = [
            java_file for java_file in java_files
            if os.path.isfile(java_file.rstrip("java") + "class")
        ]
        with open(f"{bug_dir}/java_files", 'w') as f:
            java_files_str = "\n".join(java_files_to_compile)
            f.writelines(java_files_str)
        build_cmd = f"javac -cp {class_path} @java_files"
        test_cmd = f"java -cp {class_path} Main"
        bug_id = Target.get_bug_id(bug_dir)
        target = Target.from_old_target({
            "bug_dir": bug_dir,
            "bug_id": bug_id,
            "build_cmd": build_cmd,
            "test_cmd": test_cmd
        })
        return target

    @staticmethod
    def targets_from_vfix(bench_dir):
        bench_dir = bench_dir.rstrip('/')
        targets: List[Target] = []
        bug_dirs = glob.glob(f"{bench_dir}/*-*/source")
        print(bug_dirs)
        return utils.multiprocess(Target.target_from_vfix_dir,
                                  bug_dirs,
                                  n_cpus=30)
        # Target.to_json_list(output_path, targets)

    @staticmethod
    def target_from_dir(bench_dir) -> Target:
        bug_dir = bench_dir.rstrip('/')
        test_cls = utils.read_json_from_file(f"{bug_dir}/bug.json")[
            'test_info']['testcases'][0]['classname'].split(".")[-1]
        test_mthd = utils.read_json_from_file(f"{bug_dir}/bug.json")[
            'test_info']['testcases'][0]['method'].split("{")[0]
        build_cmd = f"mvn test-compile {MVN_OPTION}"
        test_cmd = f"mvn test -DskipIT -Dtest={test_cls} -DfailIfNoTests=false {MVN_OPTION}"
        bug_id = os.path.basename(bug_dir)
        target = Target.from_old_target({
            "bug_dir": bug_dir,
            "bug_id": bug_id,
            "build_cmd": build_cmd,
            "test_cmd": test_cmd
        })
        return target

    @staticmethod
    def targets_from_apache(bench_dir):
        bench_dir = bench_dir.rstrip('/')
        targets: List[Target] = []
        bug_dirs = [
            bug_dir for bug_dir in glob.glob(f"{bench_dir}/*")
            if os.path.isfile(f"{bug_dir}/npe.json")
            and os.path.isfile(f"{bug_dir}/bug.json")
        ]
        for bug_dir in bug_dirs:
            try:
                targets.append(Target.target_from_dir(bug_dir))
            except Exception:
                print(f"{ERROR} occurs get test-info from {bug_dir}")

        return targets

    @staticmethod
    def targets_from_json_file(jsonfile) -> List[Target]:
        target_dict_list = utils.read_json_from_file(jsonfile)
        return [
            utils.from_dict(Target, target_dict)
            for target_dict in target_dict_list
        ]

    @staticmethod
    def filter_by_bugset(targets: List[Target],
                         to_include,
                         to_exclude=[]) -> List[Target]:
        targets_from_bugset = []
        to_include, to_exclude = [
            bug_id.replace("-buggy", "") for bug_id in to_include
        ], [bug_id.replace("-buggy", "") for bug_id in to_exclude]
        for target in targets:
            if target.bug_id.replace("-buggy", "") in to_exclude:
                continue
            if target.bug_id.replace("-buggy", "") in to_include:
                targets_from_bugset.append(target)
        print(f"running {len(targets_from_bugset)} targets")
        targets_not_found = set(to_include) - set([
            target.bug_id.replace("-buggy", "")
            for target in targets_from_bugset
        ]) - set(to_exclude)
        pprint(f"targets not found: {targets_not_found}")
        return targets_from_bugset

    @staticmethod
    def to_json_list(jsonfile, targets):
        if os.path.isfile(jsonfile):
            existing_targets = Target.targets_from_json_file(jsonfile)
        else:
            existing_targets = []

        targets = set(existing_targets) | set(targets)
        utils.save_dict_to_jsonfile(jsonfile,
                                    [asdict(target) for target in targets])

    def checkout(self):
        bug_dir = self.bug_dir
        # vfix_root/bug_id/source
        # apache_root/benchmarks/bug_id
        while os.path.isfile(f"{bug_dir}/../../.git/index.lock"):
            backoff = random.uniform(0.1, 2.0)
            time.sleep(backoff)

        ret = utils.execute(f"git checkout -- {bug_dir}", dir=bug_dir)
        if ret.return_code == 128:
            backoff = random.uniform(0.1, 2.0)
            time.sleep(backoff)
            self.checkout()

        elif ret.return_code != 0:
            print(f"[FAIL] git checkout")
            print(ret.stderr)
            print(ret.stdout)
            exit(-1)
