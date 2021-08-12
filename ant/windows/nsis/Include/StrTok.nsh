!define StrTok "!insertmacro StrTok"

!macro StrTok ResultVar String Separators ResultPart SkipEmptyParts
  Push "${String}"
  Push "${Separators}"
  Push "${ResultPart}"
  Push "${SkipEmptyParts}"
  Call StrTok
  Pop "${ResultVar}"
!macroend

Function StrTok
/*After this point:
  ------------------------------------------
  $0 = SkipEmptyParts (input)
  $1 = ResultPart (input)
  $2 = Separators (input)
  $3 = String (input)
  $4 = SeparatorsLen (temp)
  $5 = StrLen (temp)
  $6 = StartCharPos (temp)
  $7 = TempStr (temp)
  $8 = CurrentLoop
  $9 = CurrentSepChar
  $R0 = CurrentSepCharNum
  */

  ;Get input from user
  Exch $0
  Exch
  Exch $1
  Exch
  Exch 2
  Exch $2
  Exch 2
  Exch 3
  Exch $3
  Exch 3
  Push $4
  Push $5
  Push $6
  Push $7
  Push $8
  Push $9
  Push $R0

  ;Parameter defaults
  ${IfThen} $2 == `` ${|} StrCpy $2 `|` ${|}
  ${IfThen} $1 == `` ${|} StrCpy $1 `L` ${|}
  ${IfThen} $0 == `` ${|} StrCpy $0 `0` ${|}

  ;Get "String" and "Separators" length
  StrLen $4 $2
  StrLen $5 $3
  ;Start "StartCharPos" and "ResultPart" counters
  StrCpy $6 0
  StrCpy $8 -1

  ;Loop until "ResultPart" is met, "Separators" is found or
  ;"String" reaches its end
  ResultPartLoop: ;"CurrentLoop" Loop

  ;Increase "CurrentLoop" counter
  IntOp $8 $8 + 1

  StrSearchLoop:
  ${Do} ;"String" Loop
    ;Remove everything before and after the searched part ("TempStr")
    StrCpy $7 $3 1 $6

    ;Verify if it's the "String" end
    ${If} $6 >= $5
      ;If "CurrentLoop" is what the user wants, remove the part
      ;after "TempStr" and itself and get out of here
      ${If} $8 == $1
      ${OrIf} $1 == `L`
        StrCpy $3 $3 $6
      ${Else} ;If not, empty "String" and get out of here
        StrCpy $3 ``
      ${EndIf}
      StrCpy $R0 `End`
      ${ExitDo}
    ${EndIf}

    ;Start "CurrentSepCharNum" counter (for "Separators" Loop)
    StrCpy $R0 0

    ${Do} ;"Separators" Loop
      ;Use one "Separators" character at a time
      ${If} $R0 <> 0
        StrCpy $9 $2 1 $R0
      ${Else}
        StrCpy $9 $2 1
      ${EndIf}

      ;Go to the next "String" char if it's "Separators" end
      ${IfThen} $R0 >= $4 ${|} ${ExitDo} ${|}

      ;Or, if "TempStr" equals "CurrentSepChar", then...
      ${If} $7 == $9
        StrCpy $7 $3 $6

        ;If "String" is empty because this result part doesn't
        ;contain data, verify if "SkipEmptyParts" is activated,
        ;so we don't return the output to user yet

        ${If} $7 == ``
        ${AndIf} $0 = 1 ;${TRUE}
          IntOp $6 $6 + 1
          StrCpy $3 $3 `` $6
          StrCpy $6 0
          Goto StrSearchLoop
        ${ElseIf} $8 == $1
          StrCpy $3 $3 $6
          StrCpy $R0 "End"
          ${ExitDo}
        ${EndIf} ;If not, go to the next result part
        IntOp $6 $6 + 1
        StrCpy $3 $3 `` $6
        StrCpy $6 0
        Goto ResultPartLoop
      ${EndIf}

      ;Increase "CurrentSepCharNum" counter
      IntOp $R0 $R0 + 1
    ${Loop}
    ${IfThen} $R0 == "End" ${|} ${ExitDo} ${|}

    ;Increase "StartCharPos" counter
    IntOp $6 $6 + 1
  ${Loop}

/*After this point:
  ------------------------------------------
  $3 = ResultVar (output)*/

  ;Return output to user

  Pop $R0
  Pop $9
  Pop $8
  Pop $7
  Pop $6
  Pop $5
  Pop $4
  Pop $0
  Pop $1
  Pop $2
  Exch $3
FunctionEnd