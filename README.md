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

# Download & Installation
We provide a VirtualBox image containing all resources to reproduce the main results of our paper.
We have already setup all the environment to run the tools in the image, so we expect no further requirements.


Download link for the VirtualBox image: [[here]](link_to_vbox_image) (xx GB)

Please see [INSTALL.md](./INSTALL.md) for the full installation instructions and basic usage of NPEX.

# Reproducing the Results in the Paper
We provide a python script to reproduce the results of Table 1 in the paper.

## 
