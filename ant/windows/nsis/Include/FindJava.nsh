!include FileFunc.nsh
!include LogicLib.nsh
!include x64.nsh

!include StrRep.nsh
!include IndexOf.nsh

; Resulting variable
Var /GLOBAL java
Var /GLOBAL javaw

; Constants
!define EXE "java.exe"

!define ADOPT "SOFTWARE\Classes\AdoptOpenJDK.jarfile\shell\open\command"

!define JRE "Software\JavaSoft\Java Runtime Environment"
!define JRE32 "Software\Wow6432Node\JavaSoft\Java Runtime Environment"
!define JDK "Software\JavaSoft\JDK"
!define JDK32 "Software\Wow6432Node\JavaSoft\JDK"

; Macros
!macro _ReadAdoptKey
    ClearErrors
    ReadRegStr $0 HKLM "${ADOPT}" ""
    StrCpy $0 "$0" "" 1 ; Remove first double-quote
    ${IndexOf} $1 $0 "$\"" ; Find the index of second double-quote
    StrCpy $0 "$0" $1 ; Get the string section up to the index
    IfFileExists "$0" Found
!macroend

!macro _ReadReg key
    ClearErrors
    ReadRegStr $0 HKLM "${key}" "CurrentVersion"
    ReadRegStr $0 HKLM "${key}\$0" "JavaHome"
    IfErrors +2 0
    StrCpy $0 "$0\bin\${EXE}"
    IfFileExists "$0" Found
!macroend

!macro _ReadWorking path
    ClearErrors
    StrCpy $0 "$EXEDIR\${path}\bin\${EXE}"
    IfFileExists $0 Found
!macroend

!macro _ReadEnv var
    ClearErrors
    ReadEnvStr $0 "${var}"
    StrCpy $0 "$0\bin\${EXE}"
    IfFileExists "$0" Found
!macroend

; Create the shared function.
!macro _FindJava un
    Function ${un}FindJava
        ${If} ${RunningX64}
            SetRegView 64
        ${EndIf}

        ; Check relative directories
        !insertmacro _ReadWorking "jre"
        !insertmacro _ReadWorking "jdk"

        ; Check common env vars
        !insertmacro _ReadEnv "JAVA_HOME"

        ; Check registry
        !insertmacro _ReadAdoptKey
        !insertmacro _ReadReg "${JRE}"
        !insertmacro _ReadReg "${JRE32}"
        !insertmacro _ReadReg "${JDK}"
        !insertmacro _ReadReg "${JDK32}"

        ; Give up.  Use java.exe and hope it works
        StrCpy $0 "${EXE}"

        ; Set global var
        Found:
        StrCpy $java $0
        ${StrRep} '$java' '$java' 'javaw.exe' '${EXE}' ; AdoptOpenJDK returns "javaw.exe"
        ${StrRep} '$javaw' '$java' '${EXE}' 'javaw.exe'
    FunctionEnd
!macroend

; Allows registering identical functions for install and uninstall
!insertmacro _FindJava ""
;!insertmacro _FindJava "un."