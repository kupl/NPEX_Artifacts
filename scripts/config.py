from colorama import Fore, Style  #type:ignore

MVN_OPTION = "-V -B -Denforcer.skip=true -Dcheckstyle.skip=true -Dcobertura.skip=true -Drat.skip=true -Dlicense.skip=true -Dfindbugs.skip=true -Dgpg.skip=true -Dskip.npm=true -Dskip.gulp=true -Dskip.bower=true -Drat.numUnapprovedLicenses=100"
MVN_SKIP_TESTS = "-DskipTests=true -DskipITs=true -Dtest=None -DfailIfNoTests=false"

ERROR = f"{Fore.RED}[ERROR]{Style.RESET_ALL}"
FAIL = f"{Fore.YELLOW}[FAIL]{Style.RESET_ALL}"
WARNING = f"{Fore.MAGENTA}[WARNING]{Style.RESET_ALL}"
SUCCESS = f"{Fore.CYAN}[SUCCESS]{Style.RESET_ALL}"
TIMEOUT = f"{Fore.LIGHTMAGENTA_EX}[TIMEOUT]{Style.RESET_ALL}"
PROGRESS = f"{Fore.LIGHTWHITE_EX}[PROGRESS]{Style.RESET_ALL}"
SERIOUS = f"{Fore.LIGHTRED_EX}[SERIOUS]{Style.RESET_ALL}"

JDK_6 = "/usr/lib/jvm/jdk1.6.0_45"
JDK_7 = "/usr/lib/jvm/jdk1.7.0_80"
JDK_8 = "/usr/lib/jvm/java-8-openjdk-amd64"
JDK_11 = "/usr/lib/jvm/jdk-11.0.8"

MSG_TEST_FAIL = "test failures"
MSG_ASSERT_FAIL = "Assertion"
MSG_COMPILE_FAIL = "Compilation failure"
MSG_NPE = "NullPointerException"
