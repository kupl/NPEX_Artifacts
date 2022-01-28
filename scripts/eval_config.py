#!/usr/bin/python3.8
from __future__ import annotations
import os
from config import *

NPEX_DIR = os.getenv("NPEX_DIR")
INFER_DIR = f"{NPEX_DIR}/npex-analyzer"
CLASSIFIER_DIR = f"{os.path.dirname(os.path.abspath(__file__))}/../data/models_learned"

JDK_15 = "/usr/lib/jvm/jdk-15.0.1"
JAVA_15 = f"{JDK_15}/bin/java"
NPEX_JAR = f"{NPEX_DIR}/npex-driver/target/npex-driver-1.0-SNAPSHOT.jar"

INFER = f"{INFER_DIR}/infer/bin/infer"
NPEX_synthesizer_script = f"{NPEX_DIR}/scripts/main.py"
NPEX_analyzer_script = f"{INFER_DIR}/scripts/verify.py"
NPEX_CMD = f"{JAVA_15} --enable-preview -cp {NPEX_JAR} npex.driver.Main"

LABEL_DIR = f"/root/Workspace/data/labels"
BENCH_DIR = f"/root/Workspace/benchmarks"

N_CPUS = 10

vfix_benches = [
    "chart-14", "chart-15", "chart-16", "chart-25", "chart-26", "chart-4", "collections-360", "collections-39",
    "felix-4960", "felix-5464", "lang-20", "lang-33", "lang-39", "lang-47", "lang-57", "math-4", "pdfbox-2477",
    "pdfbox-2948", "pdfbox-2965", "pdfbox-2995", "pdfbox-3479", "pdfbox-3572", "sling-4982", "sling-6487"
]

genesis_reg = [
    "activemq-artemis_6fbafc4",
    "Activity-3d624a5-buggy",
    "async-http-client_86948f6",
    "Bears-17-buggy",
    "Bears-184-buggy",
    "Bears-32-buggy",
    "caelum-stella-2d2dd9c-buggy",
    "caelum-stella-2ec5459-buggy",
    "caelum-stella-e73113f-buggy",
    "checkstyle-536bc20-buggy",
    "checkstyle-8381754-buggy",
    "commons-configuration_746821e",
    "commons-dbcp_b137fda",
    "commons-io_1ac7bef",
    "commons-pool_41f4e41",
    "cxf_ae805e6",
    "DataflowJavaSDK-c06125d-buggy",
    "directory-ldap-api_3c6a765",
    "dubbo-hessian-lite_5526dd8",
    "error-prone-370938-buggy",
    "feign_cf31cd1",
    "httpcomponents-core_a63b121",
    "iotdb_97ca7e0",
    "javaslang-faf9ac2-buggy",
    "jspwiki_bdab6f2",
    "JSqlParser_2897935",
    "ninja_3e73cb8",
    "nutz_bbd28db",
    "opennlp_cb6ee2c",
    "OpenPDF_07ecaaf",
    "rocketmq_03c1f11",
    "shardingsphere_e46f68d",
    "shiro_3ca513f",
    "sling-org-apache-sling-pipes_674819d",
    "spring-hateoas-48749e7-buggy",
    "tablesaw_65596d8",
    "Bears-121-buggy",
    "Bears-196-buggy",
    "Bears-46-buggy",
    "aries-jpa_7712046",
    "logging-log4j2_d1c02ee",
    "pdfbox_bdab232",
]

