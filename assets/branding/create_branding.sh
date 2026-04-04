#!/bin/bash

# Creates the following icon files from SVG:
# - apple-icon.icns
# - windows-icon.ico
# - ant/windows/nsis/uninstall.ico
#
# Usage:
#   create_branding.sh [assets_color] [apple_color1] [apple_color2]
#     assets_color: Colorize the qz/ui/resources
#     apple_color1: Gradient top color for apple-icon.svg
#     apple_color1: Gradient bottom color for apple-icon.svg
#
# This file is a possible candidate for migrating to 'src/qz/build' with
# other build-time branding enhancements.

set -e

# Get working directory
DIR=$(cd "$(dirname "$0")" && pwd)
pushd "$DIR" &> /dev/null

# Handle older imagemagick versions
if ! command -v magick > /dev/null ; then
  magick() {
    convert "$@"
  }
fi

# Apple svg file to convert
apple_svg="apple-icon.svg"

# Create a temporary directory for the apple iconset
apple_icon_dir="apple-icon.iconset"
rm -rf "$apple_icon_dir"
mkdir -p "$apple_icon_dir"

# List of standard macOS icon sizes
apple_sizes=(
  16
  32
  48
  128
  256
  512
)

# Windows svg file to convert
windows_svg="linux-icon.svg"

# Windows adds a few more sizes
win_sizes=("${apple_sizes[@]}")
win_sizes+=(
  24
  64
  96
)

# Sort highest to lowest
IFS=$'\n' win_sizes=($(sort -nr <<<"${win_sizes[*]}"))
unset IFS

# Create a temporary directory for the windows iconset
windows_icon_dir="windows-icon.iconset"
rm -rf "$windows_icon_dir"
mkdir -p "$windows_icon_dir"

# First parameter: Adjust UI resource colors
pushd ../../src/qz/ui/resources &> /dev/null
icons_color="$1"
if [ -n "$icons_color" ]; then
  echo Colorizing icons to "$icons_color" as requested
  for img in about allow desktop exit field folder log reload saved settings; do
    if [ -f qz-$img.png ]; then
      echo Colorizing qz-$img.png
      magick qz-$img.png -fill "$icons_color" -colorize 100 qz-$img.png
    else
      echo qz-$img.png does not exist, skipping.
    fi
  done
else
  echo Colorize flag not set. Skipping.
fi
popd &> /dev/null

#
# Second/third parameters: Adjust macOS SVG colors
#
field="stop-color"
pattern='"[^"]*"'
cp apple-icon.svg apple-icon-temp.svg
for color in "$2" "$3"; do
  if [ -n "$color" ]; then
    color="\"$color\""
    echo "Replacing $field=... with $field=$color..."
    # Deliberately add a space every time so the current match is ignored by future passes
    sed "1,/$field=$pattern/ s/$field=$pattern/$field = $color/" apple-icon-temp.svg > apple-icon-temp-next.svg
    mv apple-icon-temp-next.svg apple-icon-temp.svg
  fi
done
# Remove the spaces we added
sed "s/stop-color = /stop-color=/g" apple-icon-temp.svg > apple-icon.svg
rm apple-icon-temp.svg

#
# Create Apple ICNS file
#
for size in "${apple_sizes[@]}"; do
  for scale in 1 2; do
    scaled=$(($size * $scale))
    suffix=""; [[ $scale -eq 2 ]] && suffix="@2x"

    name="icon_${size}x${size}${suffix}.png"
    echo "Rendering ${name} (${scaled}px)..."
    rsvg-convert -w $scaled -h $scaled -f png -o "$apple_icon_dir/$name" "$apple_svg"
  done
done

echo "Creating the apple icon apple-icon.icns..."
if command -v iconutil > /dev/null ; then
  # macos
  iconutil -c icns "$apple_icon_dir"
else
  # png2icns doesn't support retina
  mv "$apple_icon_dir/icon_512x512@2x.png" "$apple_icon_dir/icon_1024x1024.png"
  rm "$apple_icon_dir/"*@2x*
  png2icns apple-icon.icns "$apple_icon_dir/"*.png
fi

echo "Your apple icon is ready at $DIR/apple-icon.icns"

#
# Create Windows ICO file
#
windows_pngs=()
uninstall_pngs=()
for size in "${win_sizes[@]}"; do
  file="$windows_icon_dir/icon_${size}x${size}.png"
  uninstall="$windows_icon_dir/uninstall_${size}x${size}.png"
  windows_pngs+=("$file")
  uninstall_pngs+=("$uninstall")
  echo "Rendering $file (${size}px)..."
  rsvg-convert -w $size -h $size -f png -o "$file" "$windows_svg"
  echo "Rendering $uninstall (${size}px)..."
  magick "$file" -colorspace gray "$uninstall"
done

magick "${windows_pngs[@]}" windows-icon.ico
magick "${uninstall_pngs[@]}" "$DIR/../../ant/windows/nsis/uninstall.ico"

echo "Your windows icon is ready at $DIR/windows-icon.ico"
echo "Your windows uninstall icon is ready at ant/windows/nsis/uninstall.ico"

echo "Removing $windows_icon_dir"
rm -rf "$windows_icon_dir"

echo "Removing $apple_icon_dir"
rm -rf "$apple_icon_dir"

popd &> /dev/null
