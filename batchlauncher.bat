@echo off
title Huehifyer Launcher (4GB ALLOCATION)
call :MsgBox "Would you like to open Huehifyer? (This will also send you to the FFmpeg GitHub where you can install it as it's required)"  "VBYesNo+VBQuestion" "Huehfyer Launcher"
    if errorlevel 7 (
        echo Not opening Huehifyer. Press any key to exit.
        pause >nul
    ) else if errorlevel 6 (
        echo Opening Huehifyer.
        start "" https://github.com/FFmpeg/FFmpeg/releases/tag/n3.0
        java -Xmx4096m -jar huehifyer.jar
    )

    exit /b

:MsgBox prompt type title
    setlocal enableextensions
    set "tempFile=%temp%\%~nx0.%random%%random%%random%vbs.tmp"
    >"%tempFile%" echo(WScript.Quit msgBox("%~1",%~2,"%~3") & cscript //nologo //e:vbscript "%tempFile%"
    set "exitCode=%errorlevel%" & del "%tempFile%" >nul 2>nul
    endlocal & exit /b %exitCode%
   
