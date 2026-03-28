shell=$(ps -p $$ -oargs=|awk '{print $1}')
date=$(date "+%F %T")
script=$(basename "$0")

if [[ "$OSTYPE" == "darwin"* ]]; then
  user="$(eval echo ~$USER)"
else
  user="$(eval echo ~$(logname))"
fi

echo "$date Successful provisioning test from '$shell': $script" >> "$user/Desktop/provision.log"
chmod 664 "$user/Desktop/provision.log"