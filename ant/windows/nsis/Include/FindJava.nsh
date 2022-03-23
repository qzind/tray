!include FileFunc.nsh
!include LogicLib.nsh
!include x64.nsh

!include StrRep.nsh
!include IndexOf.nsh
!include StrTok.nsh

; Resulting variable
Var /GLOBAL java
Var /GLOBAL javaw
Var /GLOBAL java_major

; Constants
!define EXE "java.exe"

!define ADOPT "SOFTWARE\Classes\AdoptOpenJDK.jarfile\shell\open\command"
!define ECLIPSE "SOFTWARE\Classes\Eclipse Adoptium.jarfile\shell\open\command"
!define ECLIPSE_OLD "SOFTWARE\Classes\Eclipse Foundation.jarfile\shell\open\command"

!define JRE "Software\JavaSoft\Java Runtime Environment"
!define JRE32 "Software\Wow6432Node\JavaSoft\Java Runtime Environment"
!define JDK "Software\JavaSoft\JDK"
!define JDK32 "Software\Wow6432Node\JavaSoft\JDK"

; Macros
!macro _ReadEclipseKey
    ClearErrors
    ReadRegStr $0 HKLM "${ECLIPSE}" ""
    StrCpy $0 "$0" "" 1 ; Remove first double-quote
    ${IndexOf} $1 $0 "$\"" ; Find the index of second double-quote
    StrCpy $0 "$0" $1 ; Get the string section up to the index
    IfFileExists "$0" Found
!macroend

!macro _ReadEclipseOldKey
    ClearErrors
    ReadRegStr $0 HKLM "${ECLIPSE_OLD}" ""
    StrCpy $0 "$0" "" 1 ; Remove first double-quote
    ${IndexOf} $1 $0 "$\"" ; Find the index of second double-quote
    StrCpy $0 "$0" $1 ; Get the string section up to the index
    IfFileExists "$0" Found
!macroend

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

!macro _ReadPayload root path
    ClearErrors
    StrCpy $0 "${root}\${path}\bin\${EXE}"
    IfFileExists $0 Found
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
        ; Snag payload directory off the stack
        exch $R0

        ${If} ${RunningX64}
            SetRegView 64
        ${EndIf}

        ; Check payload directories
        !insertmacro _ReadPayload "$R0" "runtime"

        ; Check relative directories
        !insertmacro _ReadWorking "runtime"
        !insertmacro _ReadWorking "jre"

        ; Check common env vars
        !insertmacro _ReadEnv "JAVA_HOME"

        ; Check registry
        !insertmacro _ReadEclipseKey
        !insertmacro _ReadEclipseOldKey
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

        ; Discard payload directory
        pop $R0

        ; Detect java version
        nsExec::ExecToStack '"$java" -version'
        Pop $0
        Pop $1
        ; Isolate version number, e.g. "1.8.0"
        ${StrTok} $0 "$1" "$\"" "1" "1"
        ; Isolate major version
        ${StrTok} $R0 "$0" "." "0" "1"
        ; Handle old 1.x.x version format
        ${If} "$R0" == "1"
            ${StrTok} $R0 "$0" "." "1" "1"
        ${EndIf}

        ; Convert to integer
        IntOp $java_major $R0 + 0
    FunctionEnd
!macroend

; Allows registering identical functions for install and uninstall
!insertmacro _FindJava ""
;!insertmacro _FindJava "un."