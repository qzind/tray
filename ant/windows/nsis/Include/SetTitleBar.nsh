; Allow title masquerading
!define SetTitleBar "!insertmacro SetTitleBar"
!macro SetTitlebar title
  SendMessage $HWNDPARENT ${WM_SETTEXT} 0 "STR:${title}"
!macroend