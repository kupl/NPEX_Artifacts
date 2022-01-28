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


def localize(recap: bool, target: Target):
    bug_dir = target.bug_dir
    bug_id = target.bug_id
    TIMEOUT = 3600
    for report in glob.glob(f"{bug_dir}/npe*.json"):
        if os.path.basename(report) != "npe.json":
            utils.execute(f"rm {report}", dir=bug_dir)

    utils.remove_if_exists(f"{bug_dir}/.timeout_localization")
    utils.remove_if_exists(f"{bug_dir}/localizer_result.json")

    if recap:
        ret = utils.execute(
            f"python3.8 {NPEX_analyzer_script} --localize --recap",
            dir=bug_dir,
            timeout=TIMEOUT)
    else:
        ret = utils.execute(f"python3.8 {NPEX_analyzer_script} --localize ",
                            dir=bug_dir,
                            timeout=TIMEOUT)

    if ret.time > TIMEOUT:
        utils.execute(f"touch {bug_dir}/.timeout_localization", dir=bug_dir)

    reports = glob.glob(f"{bug_dir}/npe*.json")
    if os.path.isfile(f"{bug_dir}/localizer_result.json"):
        print(
            f"{SUCCESS}: to localize {len(reports)} faults for {bug_id} {ret.time:.2f}"
        )
    else:
        with open(f"{bug_dir}/.localization.result", 'w') as f:
            f.write(ret.stderr)
        print(f"{FAIL}: to localize faults for {bug_id} {ret.time:.2f}")


def generate_patch(target: Target):
    bug_dir = target.bug_dir
    bug_id = target.bug_id
    target.checkout()
    reports = glob.glob(f"{bug_dir}/npe*.json")
    if os.path.isdir(f"{bug_dir}/patches"):
        utils.execute(f"rm -rf {bug_dir}/patches", dir=bug_dir)

    if os.path.isfile(f"{bug_dir}/.spoon-model.cache"):
        utils.execute(f"rm -f {bug_dir}/.spoon-model.cache", dir=bug_dir)

    utils.execute(f"{NPEX_CMD} build {bug_dir}", dir=bug_dir)
    if os.path.isfile(f"{bug_dir}/.spoon-model.cache") is False:
        print(f"{FAIL}: to build {bug_id}")
        return

    for report in reports:
        utils.execute(f"{NPEX_CMD} patch {bug_dir} --report={report} --cached",
                      dir=bug_dir)

    patches = glob.glob(f"{bug_dir}/patches/*/patch.java")
    if patches == []:
        print(f"{FAIL}: to generate patches for {bug_id}")
    else:
        print(f"{SUCCESS}: {len(patches)} patches generated for {bug_id}")


def predict(classifiers, target: Target):
    bug_dir = target.bug_dir
    classifiers = f"{CLASSIFIER_DIR}/{target.bug_id}.classifier"
    # if os.path.isfile(classifiers) is False:
    #     print(f"{FAIL}: no classifiers given at for {target.bug_id} (no file {classifiers})")
    #     return

    utils.remove_if_exists(f"{bug_dir}/model.json")

    target.checkout()
    extract_cmd = f"{NPEX_CMD} extract-invo-context {bug_dir} -t {bug_dir}/localizer_result.json"
    utils.execute(extract_cmd, dir=bug_dir)
    if os.path.isfile(f"{bug_dir}/invo-ctx.npex.json") is False:
        print(f"{FAIL} to extract invo-context for {target.bug_id}")
        return

    utils.execute(
        f"{NPEX_synthesizer_script} predict {bug_dir} {classifiers} model.json",
        dir=bug_dir)
    if utils.is_empty_or_no_json(f"{bug_dir}/model.json"):
        print(f"{FAIL} to predict model.json for {target.bug_id}")
        utils.execute(f"rm {bug_dir}/model.json", dir=bug_dir)
        return
    else:
        print(f"{SUCCESS}: model.json generated for {target.bug_id}")


def validate(recap: bool, target_labeled: Tuple[Target, int]):
    target, i = target_labeled
    cpu_pool = i % N_CPUS
    bug_dir = target.bug_dir

    if os.path.isfile(f"{bug_dir}/learned_result.json"):
        utils.execute(f"rm {bug_dir}/learned_result.json", dir=bug_dir)

    if os.path.isfile(f"{bug_dir}/model.json") is False:
        print(f"{WARNING}: no model.json for {target.bug_id}")
        return

    if utils.is_empty_or_no_json(f"{bug_dir}/model.json"):
        print(f"{WARNING}: no model.json for {target.bug_id}")
        utils.execute(f"rm {bug_dir}/model.json", dir=bug_dir)
        return

    utils.remove_if_exists(f"{bug_dir}/.timeout")
    utils.remove_if_exists(f"{bug_dir}/result.json")
    TIMEOUT = 3600
    if recap:
        ret = utils.execute(
            f"python3.8 {NPEX_analyzer_script} --recap --cpu-pool {cpu_pool}",
            dir=bug_dir,
            timeout=TIMEOUT)
    else:
        ret = utils.execute(
            f"python3.8 {NPEX_analyzer_script} --cpu-pool {cpu_pool}",
            dir=bug_dir,
            timeout=TIMEOUT)

    if ret.time > TIMEOUT:
        utils.execute(f"touch {bug_dir}/.timeout", dir=bug_dir)

    if os.path.isfile(f"{bug_dir}/result.json") is False:
        print(f"{FAIL}: to infer specs for {target.bug_id} {ret.time:.2f}")
    else:
        utils.execute(
            f"mv {bug_dir}/result.json {bug_dir}/learned_result.json",
            dir=bug_dir)
        if os.path.isfile(f"{bug_dir}/labels.json"):
            labels = Label.labels_from_json(f"{target.bug_dir}/labels.json",
                                            target)
            result = Result.from_target(target, list(labels.values()))
            print(
                f"{SUCCESS}: to infer specs for {target.bug_id} {ret.time:.2f} {result.result}"
            )
        else:
            print(
                f"{SUCCESS}: to infer specs for {target.bug_id} {ret.time:.2f}"
            )


def manual_model(target_labeled: Tuple[Target, int]):
    target, i = target_labeled
    cpu_pool = i % N_CPUS
    bug_dir = target.bug_dir
    utils.remove_if_exists(f"{bug_dir}/result.json")

    ret = utils.execute(
        f"python3.8 {NPEX_analyzer_script} --manual_model --cpu-pool {cpu_pool}",
        dir=bug_dir)
    if os.path.isfile(f"{target.bug_dir}/result.json") is False:
        print(f"{FAIL}: to infer specs for {target.bug_id} {ret.time:.2f}")
    else:
        utils.execute(f"mv {bug_dir}/result.json {bug_dir}/manual_result.json",
                      dir=bug_dir)
        print(f"{SUCCESS}: to infer specs for {target.bug_id} {ret.time:.2f}")


def print_diff(patch_java):
    f = open(patch_java, 'r')
    patch_line = -1
    cnt = 0
    lines = list(f.readlines())
    for line in lines:
        cnt += 1
        if "NPEX_PATCH_BEGINS" in line:
            patch_line = cnt

    if patch_line == -1:
        print(f"{ERROR}: failed to patched line for {patch_java}")

    else:
        diff_line = 8
        print(
            "------------------------------------------------------------------"
        )
        for i in range(max(0, patch_line - diff_line),
                       min(patch_line + diff_line, len(lines))):
            print(lines[i])
        print(
            "------------------------------------------------------------------"
        )


def manual_labeling_bug(target: Target, labels: Dict[str, Label]):
    bug_dir = target.bug_dir
    bug_id = target.bug_id
    target.checkout()
    for label in list(labels.values()):
        patch = label.patch
        patch_id = patch.patch_id
        if not LabelOverall.require_manual(label.label):
            print(f"{label.label} does not require manual")
            continue

        patch_dir = f"{bug_dir}/patches/{patch_id}"
        if os.path.isdir(patch_dir) is False:
            # deprecated label
            continue

        print_diff(f"{patch_dir}/patch.java")
        print(f"{PROGRESS} re-label {label} of {bug_id}-{patch_id}")
        # print("------------------------------------------------------------------")
        print(
            f"label patches by correct(o) / incorrect(x), ambiguous(a), todo(t), weak_correct(wc)"
        )

        while True:
            label_from_developer = input()
            if label_from_developer == "o":
                labels[patch_id] = Label(bug_id, patch, LabelOverall.CORRECT,
                                         LabelDetail.MANUAL_CORRECT)
                break
            elif label_from_developer == "x":
                labels[patch_id] = Label(bug_id, patch,
                                         LabelOverall.OVERFITTING,
                                         LabelDetail.MANUAL_INCORRECT)
                break
            elif label_from_developer == "a":
                print(f"why it is ambiguous?")
                description = input()
                labels[patch_id] = Label(bug_id, patch, LabelOverall.AMBIGUOUS,
                                         LabelDetail.TODO, description)
                break
            elif label_from_developer == "t":
                labels[patch_id] = Label(bug_id, patch, LabelOverall.TODO,
                                         LabelDetail.TODO)
                break
            elif label_from_developer == "wc":
                labels[patch_id] = Label(bug_id, patch, LabelOverall.WC,
                                         LabelDetail.MANUAL_WC)
                break
            elif label_from_developer == "exit":
                print(f"{ERROR}: exit by interuppted")
                exit(1)
            else:
                print("INVALID SYMBOL given")
                continue

    Label.labels_to_json(labels, f"{bug_dir}/labels.json")
    Label.pretty_print_labels(labels, f"{bug_dir}/labels.csv")
    return labels


def labeling_target(target: Target):
    bug_dir = target.bug_dir
    test_cmd = target.test_cmd
    build_cmd = target.build_cmd
    bug_id = target.bug_id

    patch_dirs = [
        patch_dir for patch_dir in glob.glob(f"{bug_dir}/patches/*")
        if os.path.isfile(f"{patch_dir}/patch.java")
    ]

    labels = Label.labels_from_json(f"{bug_dir}/labels.json", target)
    print(f"{PROGRESS}: labeling {bug_id}")
    for patch_dir in patch_dirs:
        if 'contents' not in utils.read_json_from_file(
                f"{patch_dir}/patch.json"):
            continue
        patch = Patch.from_json(f"{patch_dir}/patch.json")
        patch_id = patch.patch_id
        patched_java_path = patch.original_filepath

        if patch_id not in labels:
            print(f"{ERROR}: {patch_id} could be duplicated")
            continue

        if labels[patch_id].label is not LabelOverall.TODO and labels[
                patch_id].detail is not LabelDetail.TODO:
            # print(f"{bug_id}-{patch_id} is already labeled as {labels[patch_id].label} and {labels[patch_id].detail}")
            continue

        print(
            f"{PROGRESS}: compile and test {bug_id}-{patch_id} which is labeled as {labels[patch_id].label}"
        )
        target.checkout()
        utils.execute(
            f"{NPEX_analyzer_script} --apply_patch --patch_id {patch_id}",
            dir=bug_dir)
        if target.is_vfix:
            jar_path = ':'.join(glob.glob(f"{target.root_dir}/deps/*.jar"))
            classpath = f"{jar_path}:{bug_dir}:{bug_dir}/../target/classes"
            build_cmd = f"javac -encoding ISO-8859-1 -cp {classpath} {patched_java_path}"
            test_cmd = f"java -cp {classpath} Main"

        ret_compile = utils.execute(build_cmd, dir=bug_dir)
        # if ret_compile.return_code != 0:
        # print(f"{FAIL}: not compiled {bug_id}-{patch_id}")
        # labels[patch_id] = (Label(bug_id, patch, LabelOverall.OVERFITTING, LabelDetail.COMPILE_FAIL))
        # continue

        timeout_test = ret_compile.time * 2
        ret_test = utils.execute(test_cmd, dir=bug_dir, timeout=timeout_test)

        if ret_test.time >= timeout_test:
            print(
                f"{WARNING}: testing timeout {bug_id}-{patch_id} ({ret_compile.time:.2f}, {ret_test.time:.2f})"
            )
            labels[patch_id] = (Label(bug_id, patch, LabelOverall.TIMEOUT,
                                      LabelDetail.TIMEOUT))
        elif ret_test.return_code == 0 or ret_test.return_code == 255:
            print(
                f"{SUCCESS}: passed testcase {bug_id}-{patch_id} ({ret_compile.time:.2f}, {ret_test.time:.2f})"
            )
            labels[patch_id] = (Label(bug_id, patch, LabelOverall.PASSTEST,
                                      LabelDetail.PASS_TEST))
        elif "There are test failures" in ret_test.stdout:
            print(
                f"{FAIL}: not pass testcase {bug_id}-{patch_id} ({ret_compile.time:.2f}, {ret_test.time:.2f})"
            )
            labels[patch_id] = (Label(bug_id, patch, LabelOverall.OVERFITTING,
                                      LabelDetail.TEST_FAIL))
        elif ret_compile.return_code != 0:
            print(f"{FAIL}: not compiled {bug_id}-{patch_id}")
            labels[patch_id] = (Label(bug_id, patch, LabelOverall.OVERFITTING,
                                      LabelDetail.COMPILE_FAIL))
        elif target.is_vfix:
            print(
                f"{FAIL}: not pass testcase {bug_id}-{patch_id} ({ret_compile.time:.2f}, {ret_test.time:.2f})"
            )
            labels[patch_id] = (Label(bug_id, patch, LabelOverall.OVERFITTING,
                                      LabelDetail.TEST_FAIL))
        else:
            print(
                f"{FAIL}: not pass testcase {bug_id}-{patch_id} ({ret_compile.time:.2f}, {ret_test.time:.2f})"
            )
            labels[patch_id] = (Label(bug_id, patch, LabelOverall.TODO,
                                      LabelDetail.TODO))

    if labels == {}:
        print(f"{ERROR}: no patches in {bug_dir}")
        return

    Label.labels_to_json(labels, f"{bug_dir}/labels.json")
    Label.pretty_print_labels(labels, f"{bug_dir}/labels.csv")


def prepare_target(recap: bool, target: Target):
    # if os.path.exists(f"{target.bug_dir}/localizer_result.json") is False:
    localize(recap, target)
    # if glob.glob(f"{target.bug_dir}/patches/*") == []:
    generate_patch(target)


def manual_labeling(targets, output_path):
    labels: Dict[str, Dict[str, Label]] = {}
    for target in targets:
        bug_dir = target.bug_dir
        bug_id = target.bug_id
        # skip if done
        label_map = Label.labels_from_json(f"{bug_dir}/labels.json", target)
        if label_map == {}:
            print(f"{WARNING}: no label.json in {bug_id}")
        elif all([label.is_done() for label in label_map.values()]):
            print(f"{PROGRESS}: labeling of {bug_id} is done!")
        else:
            label_map = manual_labeling_bug(target, label_map)
            Label.labels_to_json(label_map, output_path)
        labels[bug_id] = label_map
    return labels


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("--bug_id", help="patch_id")
    parser.add_argument("--apply_patch",
                        default=False,
                        action='store_true',
                        help="patch_id")
    parser.add_argument("--capture",
                        default=False,
                        action='store_true',
                        help="patch_id")
    parser.add_argument("--inference",
                        default=False,
                        action='store_true',
                        help="patch_id")
    parser.add_argument("--verify",
                        default=False,
                        action='store_true',
                        help="patch_id")
    parser.add_argument("--patch",
                        default=False,
                        action='store_true',
                        help="generate_patch")
    parser.add_argument("--recap",
                        default=False,
                        action='store_true',
                        help="re-capture infer-out")
    parser.add_argument("--testcase", help="testclass#testmethod")
    parser.add_argument("--predict",
                        default=False,
                        action='store_true',
                        help="generate model.json")
    parser.add_argument("--all",
                        default=False,
                        action='store_true',
                        help="run all bugs including no patch")
    parser.add_argument("--vfix",
                        default=False,
                        action='store_true',
                        help="run vfix bugs only")
    parser.add_argument("--vfix_rg",
                        default=False,
                        action='store_true',
                        help="run vfix bugs only")
    parser.add_argument("--vfix_comp",
                        default=False,
                        action='store_true',
                        help="run vfix runnable bugs only")
    parser.add_argument("--genesis",
                        default=False,
                        action='store_true',
                        help="run genesis runnable only")
    parser.add_argument("--genesis_rg",
                        default=False,
                        action='store_true',
                        help="run genesis runnable regression")
    parser.add_argument("--genesis_diff",
                        default=False,
                        action='store_true',
                        help="run genesis runnable regression")

    parser.add_argument('--target',
                        default=f"{DEFAULT_TARGETS}",
                        help='targets.json')
    parser.add_argument('--cpus',
                        default=10,
                        type=int,
                        help='number of cpus to use')
    parser.add_argument("--study",
                        default=False,
                        action='store_true',
                        help="study for each step")

    subparser = parser.add_subparsers(dest='subcommand')

    labeling_args = subparser.add_parser('label',
                                         help='label and generate labels.csv')
    labeling_args.add_argument("--manual",
                               action="store_true",
                               help="manually label unlabeled patches")
    labeling_args.add_argument('--output',
                               default=DEFAULT_LABELING,
                               help='benchmarks root directory')

    prepare_args = subparser.add_parser(
        'prepare', help='prepare for efficient evaluation')

    validate_args = subparser.add_parser(
        'validate', help='infer specification and validate patches')
    validate_args.add_argument("--classifiers",
                               default=None,
                               help="classifiers to extract model")
    validate_args.add_argument("--predict",
                               action="store_true",
                               help="predict null values")
    validate_args.add_argument("--verify",
                               action="store_true",
                               help="inference & validate patches")
    validate_args.add_argument("--manual_model",
                               action="store_true",
                               help="inference spec with manual model")

    evaluate_args = subparser.add_parser('evaluate', help='evaluate results')
    evaluate_args.add_argument('--labels',
                               default=DEFAULT_LABELING,
                               help='labels.json')

    args = parser.parse_args()
    targets = Target.targets_from_json_file(args.target)

    if args.bug_id:
        print(args.bug_id)
        for target in targets:
            if target.bug_id == args.bug_id:
                targets = [target]
                break
        if len(targets) != 1:
            print(f"invalid bug_id is given: {args.bug_id}")
            exit(1)
    elif args.vfix:
        targets = Target.filter_by_bugset(targets, vfix_benches)
    elif args.vfix_rg:
        targets = Target.filter_by_bugset(targets, vfix_reg)
    elif args.vfix_comp:
        targets = Target.filter_by_bugset(targets, vfix_comp)
    elif args.genesis:
        targets = Target.filter_by_bugset(targets, genesis)
    elif args.genesis_rg:
        to_run = genesis_reg  # + genesis_cand1 + genesis_cand2
        targets = Target.filter_by_bugset(targets, to_run)
    elif args.genesis_diff:
        to_run = list(set(genesis) - set(genesis_reg))
        targets = Target.filter_by_bugset(targets, to_run)
    else:
        all_bugs = [target.bug_id for target in targets]
        targets = Target.filter_by_bugset(targets, all_bugs)

    for target in targets:
        target.bug_dir = f"{ROOT_DIR}/{target.bug_dir}"
        target.root_dir = f"{ROOT_DIR}/{target.root_dir}"

    if args.subcommand == 'prepare':
        if args.study:
            target_csv = []
            for target in targets:
                n_patches = len(
                    glob.glob(f"{target.bug_dir}/patches/*/patch.java"))
                n_npes = len(glob.glob(f"{target.bug_dir}/npe*.json"))
                succ = "O" if n_patches != 0 else "X"
                row: Dict[str, Any] = {
                    "bug_id": target.bug_id,
                    "SUCC": succ,
                    "#patches": n_patches,
                    "#faults": n_npes,
                    "bug_dir": target.bug_dir
                }
                target_csv.append(row)
            target_csv.sort(key=lambda x: (x["SUCC"], x["bug_id"]))
            utils.pretty_print_dict_to_csv("prepare.results", target_csv)
        else:
            utils.multiprocess(partial(prepare_target, args.recap),
                               targets,
                               n_cpus=args.cpus)

    elif args.subcommand == 'label':
        if args.study:
            target_csv = []
            for target in targets:
                n_patches = len(
                    glob.glob(f"{target.bug_dir}/patches/*/patch.java"))
                n_npes = len(glob.glob(f"{target.bug_dir}/npe*.json"))
                patches = [
                    os.path.basename(patch_dir)
                    for patch_dir in glob.glob(f"{target.bug_dir}/patches/*")
                ]
                label_map = Label.labels_from_json(
                    f"{target.bug_dir}/labels.json", target)

                correct_patch_exists = any([
                    patch_id in label_map and label_map[patch_id].is_correct()
                    for patch_id in patches
                ])
                row = {
                    "bug_id": target.bug_id,
                    "IsTarget": target.bug_id.replace("-buggy", "")
                    in bugs_should_have_correct_patch,
                    "SUCC": correct_patch_exists,
                    "#patches": n_patches,
                    "#faults": n_npes,
                    "bug_dir": target.bug_dir
                }
                target_csv.append(row)
            target_csv.sort(key=lambda x: (x["IsTarget"], x["SUCC"], x[
                "#patches"], x["bug_id"]))
            utils.pretty_print_dict_to_csv("labeling.results", target_csv)
        elif args.manual:
            manual_labeling(targets, args.output)

        else:
            utils.multiprocess(labeling_target, targets, n_cpus=args.cpus)

    elif args.subcommand == 'validate':
        bug_dirs = [
            target['bug_dir']
            for target in utils.read_json_from_file(args.target)
        ]
        if args.study:
            target_csv = []
            for target in targets:
                n_patches = len(
                    glob.glob(f"{target.bug_dir}/patches/*/patch.java"))
                n_npes = len(glob.glob(f"{target.bug_dir}/npe*.json"))
                result = RawResult.from_json(f"{target.bug_dir}/result.json")
                row = {
                    "bug_id": target.bug_id,
                    "#faults": n_npes,
                    "#patches": n_patches,
                    # "SuccToInfer": os.path.isfile(f"{target.bug_dir}/result.json"),
                    "ModelJson":
                    os.path.isfile(f"{target.bug_dir}/model.json"),
                    "#verified": len(result.verified_patches),
                    "#rejected": len(result.rejected_patches),
                    "total_time": result.time_to_verify,
                    "bug_dir": target.bug_dir
                }
                target_csv.append(row)
            target_csv.sort(key=lambda x: (x["ModelJson"], x["bug_id"]))
            utils.pretty_print_dict_to_csv("validate.results", target_csv)
        else:
            if args.predict:
                utils.multiprocess(partial(predict, args.classifiers),
                                   targets,
                                   n_cpus=args.cpus)

            target_labeled = [(targets[i], i) for i in range(0, len(targets))]
            if args.verify:
                # validate(targets[0])
                utils.multiprocess(partial(validate, args.recap),
                                   target_labeled,
                                   n_cpus=N_CPUS)
            if args.manual_model:
                utils.multiprocess(manual_model,
                                   target_labeled,
                                   n_cpus=args.cpus)

    elif args.subcommand == 'evaluate':
        print("evaluate")
        results = []
        for target in targets:
            labels = Label.labels_from_json(
                f"{ROOT_DIR}/labels/{target.bug_id}.json", target)
            result = Result.from_target(target, list(labels.values())).asdict()
            results.append(result)
        results.sort(key=lambda x:
                     (x["source"], x["bug_class"], x['bug_id'], x['result']))
        # results.sort(key=lambda x: (x['result'], x['bug_id']))
        # results.sort(key=lambda x: (x["bug_class"], x['result'], x['manual_result'], x['bug_id']))
        utils.pretty_print_dict_to_csv("evaluate.results", results)

    # def count(target: Target):
    #     s = f"{target.bug_dir}, {utils.size_of(target.bug_dir)}"
    #     print(s)
    #     return s

    # sum = 0
    # for target in targets:
    #     if os.path.exists(f"{target.bug_dir}/localizer_result.json"):
    #         sum += utils.read_json_from_file(f"{target.bug_dir}/localizer_result.json")["time"]
    # print(sum / 118)

    # results = utils.multiprocess(count, targets, n_cpus=20)
    # print(results)
