$shell="PowerShell"
$date="$(Get-Date -format "yyyy-MM-dd HH:mm:ss")"
$script="$($myInvocation.MyCommand.Name)"
# FIXME: ~/Desktop may try to write to /root/Desktop on Linux
echo "$date Successful provisioning test from '$shell': $script" >> ~/Desktop/provision.log