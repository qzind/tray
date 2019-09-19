!define IndexOf "!insertmacro IndexOf"

!macro IndexOf Var Str Char
    Push "${Char}"
    Push "${Str}"

    Exch $R0
    Exch
    Exch $R1
    Push $R2
    Push $R3

     StrCpy $R3 $R0
     StrCpy $R0 -1
     IntOp $R0 $R0 + 1
      StrCpy $R2 $R3 1 $R0
      StrCmp $R2 "" +2
      StrCmp $R2 $R1 +2 -3

     StrCpy $R0 -1

    Pop $R3
    Pop $R2
    Pop $R1
    Exch $R0

    Pop "${Var}"
!macroend