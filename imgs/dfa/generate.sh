#!/bin/bash

# Get the directory of the script
dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# Navigate to the script directory
cd "$dir" || exit

# Iterate over all .gv files in the directory
for file in *.gv; do
    if [ -f "$file" ]; then
        # Extract filename without extension
        filename="${file%.*}"

        # Execute dot command
        dot -Tpng -o "$filename.png" "$file"
        echo "Generated $filename.png"
    fi
done


exit