#!/usr/bin/env python3
"""
Fix all invalid GIDs (>= 4097) in TMX files by replacing them with 0 (empty tile).
This handles the tileset mismatch issue where external tilesets are not found.
"""

import re
import sys
from pathlib import Path

def fix_tmx_file(filepath):
    """Replace all GIDs >= 4097 with 0 in a TMX file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        original_content = content

        # Match CSV data lines in <data> sections
        # Replace any number >= 4097 with 0
        def replace_gid(match):
            csv_line = match.group(1)
            # Split by comma, process each GID
            gids = csv_line.split(',')
            fixed_gids = []
            for gid_str in gids:
                gid_str = gid_str.strip()
                try:
                    gid = int(gid_str)
                    # Replace any GID >= 4097 with 0
                    if gid >= 4097:
                        fixed_gids.append('0')
                    else:
                        fixed_gids.append(gid_str)
                except ValueError:
                    fixed_gids.append(gid_str)
            return ','.join(fixed_gids)

        # Process each line in the data section
        lines = content.split('\n')
        fixed_lines = []
        in_data_section = False

        for line in lines:
            if '<data' in line:
                in_data_section = True
                fixed_lines.append(line)
            elif '</data>' in line:
                in_data_section = False
                fixed_lines.append(line)
            elif in_data_section and line.strip() and not line.strip().startswith('<'):
                # This is a CSV data line
                gids = line.split(',')
                fixed_gids = []
                for gid_str in gids:
                    gid_str_stripped = gid_str.strip()
                    try:
                        gid = int(gid_str_stripped)
                        if gid >= 4097:
                            fixed_gids.append('0')
                        else:
                            fixed_gids.append(gid_str)
                    except ValueError:
                        fixed_gids.append(gid_str)
                fixed_lines.append(','.join(fixed_gids))
            else:
                fixed_lines.append(line)

        content = '\n'.join(fixed_lines)

        if content != original_content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✓ Fixed: {filepath}")
            return True
        else:
            print(f"  No changes needed: {filepath}")
            return False
    except Exception as e:
        print(f"✗ Error processing {filepath}: {e}")
        return False

def main():
    base_path = Path('/home/franz/Documents/Schule/3-Klasse/POS/DroidRampage')

    source_dir = base_path / 'src/main/resources/assets/levels'
    target_dir = base_path / 'target/classes/assets/levels'

    tmx_files = list(source_dir.glob('*.tmx'))

    if not tmx_files:
        print(f"No TMX files found in {source_dir}")
        return

    print(f"Found {len(tmx_files)} TMX files")
    print("=" * 60)

    fixed_count = 0

    # Fix source files
    print("\nProcessing source files:")
    for tmx_file in sorted(tmx_files):
        if fix_tmx_file(tmx_file):
            fixed_count += 1

    # Sync to target
    print("\n" + "=" * 60)
    print("Syncing to target/classes:")
    for tmx_file in sorted(tmx_files):
        target_file = target_dir / tmx_file.name
        if target_file.exists():
            import shutil
            shutil.copy2(tmx_file, target_file)
            print(f"✓ Synced: {target_file}")

    print("\n" + "=" * 60)
    print(f"Done! Fixed {fixed_count} files.")

if __name__ == '__main__':
    main()

