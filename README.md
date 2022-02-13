# NPEX
NPEX is an automated program repair system for Null Pointer Exceptions (NPE) in Java.
The key feature of NPEX is that it automatically inferences repair specification of the buggy program and uses
the inferred specification to validate patches. This feature enables NPEX to repair an NPE bug with only
the buggy program and the corresponding NPE stack trace.
In contrast to NPEX, the state-of-the-art NPE repair techniques requires test case written by developers
as additional inputs for patch validation.
The key idea of NPEX is to learn a statistical model that predicts how developers would handle
NPEs by mining null-handling patterns from existing codebases, and to use a variant of
symbolic execution that can infer the repair specification from the buggy program using the model.
For more technical details, please consult our paper.

# Contents of the Artifact
The artifact contains the following:
* Benchmarks (`~/Workspace/benchmarks/`): all the benchmarks we used in our paper's experiment.
* Tools (`~/Workspace/tools/`): A source code of our tool (NPEX) and a modified version of Genesis in binary (JAR). We could not provide
VFix as it is not publically available (we obtained its executable via personal contacts with the authors).
* Scripts (`~/Workspace/scripts/`): Glue codes for NPEX system and python scripts to conviniently reprocude our paper's results. 
* Data (`~/Workspace/data/`): All experimental data of NPEX, VFix, Genesis.
  * NPEX's learned model (`~/Workspace/data/models_learned`): a set of learned model used in our experiment.
  * NPEX's raw results (`~/Workspace/data/raw_results/npex`)
  * Patch correctness (`~/Workspace/data/labels`): correctness results that we manually evaluated for NPEX-generated patches.
  * VFix's raw results (`~/Workspace/data/raw_results/vfix`) 
  * Genesis's raw results (`~/Workspace/data/raw_results/genesis`)

# Download & Installation
We have packaged our artifacts in a Docker image containing all resources to reproduce the main results of our paper.
We have already setup all the environment to run the tool in the image, so we expect no further requirements except for hard disk size;
about 150GB of storage space is required to fully reproduce our results.

* Download link for the Docker image: [[here]](https://figshare.com/articles/dataset/A_Replication_Package_for_NPEX_Repairing_Java_Null_Pointer_Exceptions_without_Tests_/19087652/2?file=34052735) (20 GB)
* The source codes of NPEX are also available on GitHub: [[here]](https://github.com/kupl/NPEX)

After downloading the docker image tar (npex-artifacts.tar), please run the following command to load a docker image:
```
docker load -i npex-artifacts.tar
```
You will get the id of the image on the shell:
```
Loaded image ID: sha256:574c743ea07b...
```
After then, please run the following command to run a container from the image:
```
docker run -e LC_ALL=C.UTF-8 -v /etc/localtime:/etc/localtime:ro -e TZ=Asia/Seoul -it 574c743ea07b /bin/bash
```


Please see [INSTALL.md](./INSTALL.md) for the full installation instructions and basic usage of NPEX.

# Reproducing Our Results in the Paper
We provide a python script to reproduce the results of Table 2 in the paper.
We expect all commands are executed on `~/Workspace` directory.

### Build all benchmarks
```
python3.8 scripts/run.py prepare
```
All benchmarks should be successfully built in this procedure.

### Running NPEX
```
python3.8 scripts/run.py run 
```
This procedure takes quite a long time since we have 119 benchmarks.

The results of NPEX are stored as `result.json` for each bug directory (e.g., `~/Workspace/benchmarks/Ours/aries-jpa_7712046/result.json').
This procedures contains all steps of NPEX's patch generation: 
(1) fault-localization, (2) patch enumeration, (3) specification inference, and (4) patch validation.
We combine all steps to single script for conveniently reproducing results. 
For those who want to run NPEX step by step, please see INSTALL.md.

### Generating Table
```
python3.8 scripts/run.py evaluate 
```
This script collects all our results (i.e., patch validation results for each bug) and evaluate the results whether the validated patch is correct or not by `Workspace/data/labels`. The result table is stored at `evaluate.results`. 



