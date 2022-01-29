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
For more technical details, please consult our [paper](link_to_paper).

# Contents of the Artifact
The artifact (provided in form of an VirtualBox image) contains the following:
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

* Download link for the Docker image: [[here]](https://doi.org/10.6084/m9.figshare.19087652.v1) (18 GB)
* The source codes of NPEX are also available on GitHub: [[here]](https://github.com/kupl/NPEX)


Please see [INSTALL.md](./INSTALL.md) for the full installation instructions and basic usage of NPEX.

# Reproducing Our Results in the Paper
We provide a python script to reproduce the results of Table 2 in the paper.
### Build all benchmarks
```
cd benchmarks && python3.8 scripts/run.py prepare
```
We expect all commands are executed on `~/Workspace/benchmarks` directory.

### Running NPEX
```
python3.8 ../scripts/run.py run 
```
The results of NPEX are stored as `result.json` for each bug directory (e.g., `Workspace/benchmarks/Ours/aries-jpa_7712046/result.json').
This procedures contains all steps of NPEX's patch generation: 
(1) fault-localization, (2) patch enumeration, (3) specification inference, and (4) patch validation.
We combine all steps to single script for conveniently reproducing results. 
For those who want to run NPEX step by step, please see INSTALL.md.

### Generating Table
```
python3.8 ../scripts/run.py evaluate 
```
This script collects all our results (i.e., patch validation results for each bug) and evaluate the results whether the validated patch is correct or not by `Workspace/data/labels`. The result table is stored at `evaluate.results`. 



