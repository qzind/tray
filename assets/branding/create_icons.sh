#!/bin/bash

set -e

# Get working directory
DIR=$(cd "$(dirname "$0")" && pwd)
pushd "$DIR" &> /dev/null

# Apple svg file to convert
apple_svg="apple-icon.svg"

# Create a temporary directory for the apple iconset
apple_icon_dir="apple-icon.iconset"
rm -rf "$apple_icon_dir"
mkdir -p "$apple_icon_dir"

# List of standard macOS icon apple_sizes
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

# Create a temporary directory for the apple iconset
windows_icon_dir="windows-icon.iconset"
rm -rf "$windows_icon_dir"
mkdir -p "$windows_icon_dir"

# Sort
IFS=$'\n' win_sizes=($(sort -nr <<<"${win_sizes[*]}"))
unset IFS

# Create Apple ICNS file
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

# Create Windows ICO file
for size in "${win_sizes[@]}"; do
  file="$windows_icon_dir/icon_${size}x${size}.png"
  echo "Rendering $file (${size}px)..."
  rsvg-convert -w $size -h $size -f png -o "$file" "$windows_svg"
done

if ! command -v magick > /dev/null ; then
  # Handle older versions
  magick() {
    convert "$@"
  }
fi
magick "$windows_icon_dir"/*.png windows-icon.ico

echo "Your windows icon is ready at $DIR/windows-icon.ico"

# Create Windows uninstall ICO file
pushd "$windows_icon_dir" &> /dev/null
for f in *.png; do
  echo "Rendering uninstall-$f..."
  magick "$f" -colorspace gray "uninstall-$f"
done
magick uninstall-*.png "$DIR/../../ant/windows/nsis/uninstall.ico"
popd &> /dev/null

echo "Your windows uninstall icon is ready at ant/windows/nsis/uninstall.ico"

echo "Removing $windows_icon_dir"
rm -rf "$windows_icon_dir"

echo "Removing $apple_icon_dir"
rm -rf "$apple_icon_dir"

popd &> /dev/null
