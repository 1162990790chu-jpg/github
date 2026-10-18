@echo off
setlocal
set "SPEEDAPPLE_MCREATOR=%LOCALAPPDATA%\Programs\MCreator-2026.2\MCreator20262\mcreator.exe"
if exist "%SPEEDAPPLE_MCREATOR%" (
    "%SPEEDAPPLE_MCREATOR%" "%~dp0speedapple.mcreator"
) else (
    echo Open speedapple.mcreator in MCreator 2026.2 with the Fabric 26.2 generator installed.
    pause
    exit /b 1
)
