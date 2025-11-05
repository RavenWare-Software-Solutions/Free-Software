@echo off
setlocal

:: Set base paths
set JDK_PATH=%~dp0Tools\build-tools\jdk8\bin
set JRE_PATH=%~dp0Tools\build-tools\jdk8\jre\bin
set BASE_LOG_DIR=%~dp0Docs\java8
set BASE_BACKUP_DIR=%~dp0Repo\java8\backups
set BASE_SRC_DIR=%~dp0Repo\java8

:: Prompt for project name
set /p PROJECT_NAME=Enter project name: 
if "%PROJECT_NAME%"=="" (
    echo ERROR: Project name cannot be empty
    pause
    exit /b 1
)

:: Set project-specific paths
set PROJECT_DIR=%BASE_SRC_DIR%\%PROJECT_NAME%
set SRC_DIR=%PROJECT_DIR%\src
set BIN_DIR=%PROJECT_DIR%\bin
set LIBS_DIR=%PROJECT_DIR%\libs
set PROTO_DIR=%PROJECT_DIR%\proto
set LOG_DIR=%BASE_LOG_DIR%\%PROJECT_NAME%_logs
set LOG_FILE=%LOG_DIR%\%PROJECT_NAME%.log
set BACKUP_DIR=%BASE_BACKUP_DIR%\%PROJECT_NAME%
set JAR_FILE=%PROTO_DIR%\%PROJECT_NAME%.jar

:: Define classpath for compilation (include bin and any JARs in libs)
set CLASSPATH=%BIN_DIR%
for %%i in ("%LIBS_DIR%\*.jar") do set CLASSPATH=%CLASSPATH%;%%i

:: Create logs directory if it doesn't exist
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

:: Delete existing log file if it exists
if exist "%LOG_FILE%" del "%LOG_FILE%"

:: Log start time and environment to new log file
echo [%DATE% %TIME%] Starting run.bat for project %PROJECT_NAME% > "%LOG_FILE%"
echo Project root: %~dp0 >> "%LOG_FILE%"
echo Expected JDK path: %JDK_PATH%\javac.exe >> "%LOG_FILE%"
echo Expected JRE path: %JRE_PATH%\java.exe >> "%LOG_FILE%"
echo Expected src path: %SRC_DIR% >> "%LOG_FILE%"
echo Expected bin path: %BIN_DIR% >> "%LOG_FILE%"
echo Expected libs path: %LIBS_DIR% >> "%LOG_FILE%"
echo Expected proto path: %PROTO_DIR% >> "%LOG_FILE%"
echo Backup directory: %BACKUP_DIR% >> "%LOG_FILE%"
echo JAR file output: %JAR_FILE% >> "%LOG_FILE%"

:: Check for JDK folder
if not exist "%JDK_PATH%\javac.exe" (
    echo [%DATE% %TIME%] ERROR: JDK not found at %JDK_PATH%\javac.exe >> "%LOG_FILE%"
    echo ERROR: JDK not found at %JDK_PATH%\javac.exe
    echo [%DATE% %TIME%] Checking for folders in %JDK_PATH%\.. >> "%LOG_FILE%"
    dir "%JDK_PATH%\.." /b >> "%LOG_FILE%" 2>&1
    echo Please check the JDK folder name and update JDK_PATH in run.bat.
    echo Current folder listing:
    dir "%JDK_PATH%\.." /b
    echo [%DATE% %TIME%] Suggested action: Ensure OpenJDK is installed at %JDK_PATH% >> "%LOG_FILE%"
    pause
    exit /b 1
)

:: Check for JRE folder
if not exist "%JRE_PATH%\java.exe" (
    echo [%DATE% %TIME%] ERROR: JRE not found at %JRE_PATH%\java.exe >> "%LOG_FILE%"
    echo ERROR: JRE not found at %JRE_PATH%\java.exe
    echo [%DATE% %TIME%] Checking for folders in %JRE_PATH%\.. >> "%LOG_FILE%"
    dir "%JRE_PATH%\.." /b >> "%LOG_FILE%" 2>&1
    echo Please check the JRE folder name and update JRE_PATH in run.bat.
    echo Current folder listing:
    dir "%JRE_PATH%\.." /b
    echo [%DATE% %TIME%] Suggested action: Ensure OpenJDK JRE is installed at %JRE_PATH% >> "%LOG_FILE%"
    pause
    exit /b 1
)

:: Create project directories if project doesn't exist
if not exist "%PROJECT_DIR%" (
    echo [%DATE% %TIME%] Creating project directory at %PROJECT_DIR% >> "%LOG_FILE%"
    echo Creating project directory at %PROJECT_DIR%
    mkdir "%PROJECT_DIR%"
    mkdir "%SRC_DIR%"
    mkdir "%BIN_DIR%"
    mkdir "%LIBS_DIR%"
    mkdir "%PROTO_DIR%"
) else (
    echo [%DATE% %TIME%] Using existing project directory at %PROJECT_DIR% >> "%LOG_FILE%"
    echo Using existing project directory at %PROJECT_DIR%
    :: Create src, bin, libs, and proto if they don't exist in existing project
    if not exist "%SRC_DIR%" (
        echo [%DATE% %TIME%] Creating source directory at %SRC_DIR% >> "%LOG_FILE%"
        echo Creating source directory at %SRC_DIR%
        mkdir "%SRC_DIR%"
    )
    if not exist "%BIN_DIR%" (
        echo [%DATE% %TIME%] Creating bin directory at %BIN_DIR% >> "%LOG_FILE%"
        echo Creating bin directory at %BIN_DIR%
        mkdir "%BIN_DIR%"
    )
    if not exist "%LIBS_DIR%" (
        echo [%DATE% %TIME%] Creating libs directory at %LIBS_DIR% >> "%LOG_FILE%"
        echo Creating libs directory at %LIBS_DIR%
        mkdir "%LIBS_DIR%"
    )
    if not exist "%PROTO_DIR%" (
        echo [%DATE% %TIME%] Creating proto directory at %PROTO_DIR% >> "%LOG_FILE%"
        echo Creating proto directory at %PROTO_DIR%
        mkdir "%PROTO_DIR%"
    )
)

:: Create blank Java file if it doesn't exist
if not exist "%SRC_DIR%\%PROJECT_NAME%.java" (
    echo [%DATE% %TIME%] Creating blank %PROJECT_NAME%.java in %SRC_DIR% >> "%LOG_FILE%"
    echo Creating blank %PROJECT_NAME%.java in %SRC_DIR%
    type nul > "%SRC_DIR%\%PROJECT_NAME%.java"
)

:: Open the Java file in the default text editor
echo [%DATE% %TIME%] Opening %PROJECT_NAME%.java in default text editor >> "%LOG_FILE%"
echo Opening %PROJECT_NAME%.java in default text editor...
start /wait "" "%SRC_DIR%\%PROJECT_NAME%.java"

:: Prompt user to compile
set /p READY=Are you ready to compile? Press ENTER for yes, or Ctrl+C to cancel: 
echo [%DATE% %TIME%] User confirmed compilation >> "%LOG_FILE%"

:: Create backup directory if it doesn't exist
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

:: Determine backup version
set "BACKUP_VERSION=%PROJECT_NAME% v0.000 Base"
if exist "%BACKUP_DIR%\%PROJECT_NAME% v0.000 Base" (
    set /a VERSION_NUM=0
    for /f "tokens=3" %%i in ('dir "%BACKUP_DIR%\%PROJECT_NAME% v*" /b ^| findstr /r "%PROJECT_NAME% v[0-9]*\.[0-9][0-9][0-9]$"') do (
        set /a TEMP_NUM=%%i
        if !TEMP_NUM! gtr !VERSION_NUM! set /a VERSION_NUM=!TEMP_NUM!
    )
    set /a VERSION_NUM+=1
    set "BACKUP_VERSION=%PROJECT_NAME% v0.%VERSION_NUM:~-3%"
)

:: Backup entire project directory before compilation
echo [%DATE% %TIME%] Backing up project directory to %BACKUP_DIR%\%BACKUP_VERSION% >> "%LOG_FILE%"
echo Backing up project directory to %BACKUP_DIR%\%BACKUP_VERSION%...
if not exist "%BACKUP_DIR%\%BACKUP_VERSION%" mkdir "%BACKUP_DIR%\%BACKUP_VERSION%"
xcopy "%PROJECT_DIR%\*" "%BACKUP_DIR%\%BACKUP_VERSION%\" /E /I /Y >> "%LOG_FILE%" 2>&1

:: Compile Java source file
echo [%DATE% %TIME%] Compiling %PROJECT_NAME%.java from %SRC_DIR% to %BIN_DIR% >> "%LOG_FILE%"
echo Compiling %PROJECT_NAME%.java from %SRC_DIR% to %BIN_DIR%...
"%JDK_PATH%\javac.exe" -d "%BIN_DIR%" -cp "%CLASSPATH%" "%SRC_DIR%\%PROJECT_NAME%.java" >> "%LOG_FILE%" 2>&1

:: Check if compilation was successful
if %ERRORLEVEL% neq 0 (
    echo [%DATE% %TIME%] Compilation failed! Check %LOG_FILE% for details. >> "%LOG_FILE%"
    echo Compilation failed! Check %LOG_FILE% for details.
    pause
    exit /b %ERRORLEVEL%
)

:: Create manifest file for JAR
echo [%DATE% %TIME%] Creating manifest file for JAR >> "%LOG_FILE%"
echo Creating manifest file for JAR...
(
    echo Manifest-Version: 1.0
    echo Main-Class: %PROJECT_NAME%
    if exist "%LIBS_DIR%\*.jar" (
        echo Class-Path: %PROJECT_NAME%/libs/zxing-core-3.5.3.jar %PROJECT_NAME%/libs/zxing-javase-3.5.3.jar
    )
) > "%BIN_DIR%\MANIFEST.MF"

:: Create JAR file in proto directory
echo [%DATE% %TIME%] Creating %PROJECT_NAME%.jar in %PROTO_DIR% >> "%LOG_FILE%"
echo Creating %PROJECT_NAME%.jar in %PROTO_DIR%...
cd /d "%PROJECT_DIR%"
"%JDK_PATH%\jar.exe" cfm "%JAR_FILE%" "%BIN_DIR%\MANIFEST.MF" -C "%BIN_DIR%" . -C "%LIBS_DIR%" . >> "%LOG_FILE%" 2>&1

:: Check if JAR creation was successful
if %ERRORLEVEL% neq 0 (
    echo [%DATE% %TIME%] JAR creation failed! Check %LOG_FILE% for details. >> "%LOG_FILE%"
    echo JAR creation failed! Check %LOG_FILE% for details.
    pause
    exit /b %ERRORLEVEL%
)

:: Run the JAR file
echo [%DATE% %TIME%] Running %PROJECT_NAME%.jar from %PROTO_DIR% >> "%LOG_FILE%"
echo Running %PROJECT_NAME%.jar from %PROTO_DIR%...
cd /d "%PROTO_DIR%"
"%JRE_PATH%\java.exe" -jar "%JAR_FILE%" >> "%LOG_FILE%" 2>&1

:: Check if execution was successful
if %ERRORLEVEL% neq 0 (
    echo [%DATE% %TIME%] Execution failed! Check %LOG_FILE% for details. >> "%LOG_FILE%"
    echo Execution failed! Check %LOG_FILE% for details.
    pause
    exit /b %ERRORLEVEL%
)

:: User prompt loop
:prompt_loop
echo [%DATE% %TIME%] Prompting user for post-execution options >> "%LOG_FILE%"
echo.
echo Post-execution options:
echo 1. See compiler log
echo 2. Open project directory
echo 3. Open dependency directory
echo Press ENTER to close compiler
set /p CHOICE=Select an option (1-3, or ENTER to close compiler): 
echo [%DATE% %TIME%] User selected option: %CHOICE% >> "%LOG_FILE%"

if "%CHOICE%"=="" (
    echo [%DATE% %TIME%] User chose to close compiler >> "%LOG_FILE%"
    goto end
)
if "%CHOICE%"=="1" (
    if exist "%LOG_FILE%" (
        echo [%DATE% %TIME%] Opening compiler log %LOG_FILE% >> "%LOG_FILE%"
        start "" "%LOG_FILE%"
    ) else (
        echo [%DATE% %TIME%] ERROR: Log file %LOG_FILE% not found >> "%LOG_FILE%"
        echo ERROR: Log file not found
    )
    goto prompt_loop
)
if "%CHOICE%"=="2" (
    if exist "%PROJECT_DIR%" (
        echo [%DATE% %TIME%] Opening project directory %PROJECT_DIR% >> "%LOG_FILE%"
        start explorer "%PROJECT_DIR%"
    ) else (
        echo [%DATE% %TIME%] ERROR: Project directory %PROJECT_DIR% not found >> "%LOG_FILE%"
        echo ERROR: Project directory not found
    )
    goto prompt_loop
)
if "%CHOICE%"=="3" (
    if exist "%LIBS_DIR%" (
        echo [%DATE% %TIME%] Opening dependency directory %LIBS_DIR% >> "%LOG_FILE%"
        start explorer "%LIBS_DIR%"
    ) else (
        echo [%DATE% %TIME%] ERROR: Dependency directory %LIBS_DIR% not found >> "%LOG_FILE%"
        echo ERROR: Dependency directory not found
    )
    goto prompt_loop
) else (
    echo [%DATE% %TIME%] Invalid option selected: %CHOICE% >> "%LOG_FILE%"
    echo Invalid option: %CHOICE%. Please select 1, 2, 3, or ENTER to close compiler.
    goto prompt_loop
)

:end
echo [%DATE% %TIME%] Program executed successfully >> "%LOG_FILE%"
echo Program executed successfully
endlocal
pause
