# Download & Installation
우리는 NPEX 설치를 위해 (1) 모든 디펜던시와 실행 환경이 셋업된 VirtualBox image와 (2) 툴 소스코드로 부터 빌드하는
두 가지 옵션을 제공한다.
버츄얼 박스 이미지에는 실험 결과 재현을 위한 벤치마크, 다른 툴, 실험 스크립트 등이 포함되어 있기 때문에,
실험 결과 재현을 원하는 사람 (e.g., Artifact Reviewers)은 이 옵션을 추천한다.
<!-- We provide a VirtualBox image to install NPEX, which contains all the benchmarks, dependencies, and other tools
Also, we have wrote some scripts to conveniently reproduce our paper's results. -->

## Installation from a VirtualBox image



## Build from Source Codes
### Requirements:
  * Java 15+
  * Maven 3.6+
  * Ocaml 4.10+
  * Python 3.8+
  * Opam 2+
  * Sqlite 3+
### Building Instructions:
  1. Clone NPEX Sources from GitHub and go into the cloned directory:
      ```
      git clone https://github.com/kupl/NPEX --recursive && cd ./NPEX
      ```
		Please don't forget the `--recursive` option because we manage our specification inference & validation module
		as git submodules. The option will automatically checkouts its latest version.

  2. Build NPEX Java modules. We implemented those modules using the maven build system.
  Please run the following command:
     ```
     mvn compile
     ```
  3. Build the specification inference & validation module, which is implemented on top of the [Infer](inferlink) framework. 
  	Please run the following command:
      ```
      cd npex-analyzer && ./build-infer.sh java
      ```


# Running NPEX
NPEX를 이용하여 당신이 원하는 NPE 버그를 수정하는데 방법을 설명한다. 우리는 벤치마크 중 하나인 Math-4 프로젝트를 가지고
설명할것이다.

## Step 1. Preparing Inputs
NPEX는 다음 세 가지 입력을 받는다:
* Buggy Project: Buggy 프로젝트 그자체. 이후 명령어들은 모두 Buggy 프로젝트의 루트 디렉토리 위에서 실행되는 것을 가정한다.
  * NPEX는 Maven, Ant 빌드 시스템을 사용하는 프로젝트를 지원하고, 이외의 프로젝트 (빌드 시스템이 없는 프로젝트를 포함한)에 대해서는 XXX..
* NPE Information: NPEX는 NPE가 발생하는 소스파일의 상대 경로 (루트 기준), 라인, 그리고 대상 null pointer를 필요로 한다.
  이 정보를 JSON 포맷의 파일로 기술한다.
	아래는 Math-4 프로젝트의 NPE 정보이다.
	```
	{
		"filepath": "org/apache/commons/math3/geometry/euclidean/threed/Line.java",
		"line": 115,
		"deref_field": "point",
	}
	```
* NPE Stack Trace: Fault Localization을 위한 NPE Stack Trace가 필요. 다음은 Math-4 프로젝트의 NPE Stack Trace 예시임.
	```
	Exception in thread "main" java.lang.NullPointerException
        at org.apache.commons.math3.geometry.euclidean.threed.Line.getAbscissa(Line.java:114)
        at org.apache.commons.math3.geometry.euclidean.threed.Line.toSubSpace(Line.java:129)
        at org.apache.commons.math3.geometry.euclidean.threed.SubLine.intersection(SubLine.java:116)
        at Main.testIntersectionNotIntersecting(Main.java:16)
        at Main.main(Main.java:10)
	```  

## Step 2. Fault Localization
유저가 입력한 `npe.json`과 Stack Trace를 바탕으로, 간단한 정적 분석 기반의 Fault Localization 수행.
먼저 Stack Trace로부터 execution trace를 추출한다. 이를 위해 다음 커맨드를 실행:
```
머시기.py 머시기 저시기
```
실행이 끝나면 프로젝트 루트 디렉토리에 `traces.json`이 생성된다. 이제 이를 이용하여 Fault Localization을 실행한다:
```
라도레솔시
```
루트 디렉토리에 `npe_`를 prefix로 갖는 JSON 파일들이 생성된다.


## Step 3. Patch Generation
패치 합성기의 입력은 프로젝트 루트 디렉토리와 NPE 정보 (`npe.json`)이다. 전 단계에서 생성한 Localization된 fault를 입력으로
사용한다. 패치 합성을 위해 다음 커맨드를 실행하자:
```
$NPEX_SCRIPTS/master.py patch npe*.json
```
합성된 패치들은 `patches` 디렉토리에 생성된다.


## Step 4. Specification Inference & Patch Validation
### Specification Inference
다음 명령어를 입력하여 명세를 추론한다:
```
추론 커맨드
```
어쩌구저쩌구가 생긴다.

### Patch Validation with Inferred Specification
추론된 명세를 가지고 각 패치를 검증한다. 이를 위해 다음 커맨드를 입력:
```
검증 커맨드
```
검증이 끝나면 다음과 같이 각 패치별로 검증기 통과 유무가 기록된 테이블이 나온다:
```
XXX O XXX ...
```
