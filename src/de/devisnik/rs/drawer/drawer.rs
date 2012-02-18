// Copyright (C) 2011 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#pragma version(1)
#pragma rs java_package_name(de.devisnik.rs.drawer)

#include "rs_graphics.rsh"

typedef struct __attribute__((packed, aligned(4))) Tile {
    int2 position;
    int2 size;
    rs_allocation texture;
} Tile_t;
Tile_t *tiles;

rs_program_fragment gProgramFragment;

// gTouchX and gTouchY are variables that will be reflected for use
// by the java API. We can use them to notify the script of touch events.
int gTouchX;
int gTouchY;

// This is invoked automatically when the script is created
void init() {
    gTouchX = 50.0f;
    gTouchY = 50.0f;
}

void renderTile(uint32_t index) {
	Tile_t tile = tiles[index];
	rsgBindTexture(gProgramFragment, 0, tile.texture);
	rsgDrawRect(tile.position.x, tile.position.y, tile.position.x+tile.size.x, tile.position.y+tile.size.y, 0);	
}

int root() {

    // Clear the background color
    rsgClearColor(0.0f, 0.0f, 0.0f, 0.0f);

	rsgBindProgramFragment(gProgramFragment);

    uint32_t dimX = rsAllocationGetDimX(rsGetAllocation(tiles));
    for (uint32_t ct=0; ct < dimX; ct++) {
		renderTile(ct);		
    }

    tiles->position.x+= 1;
    tiles->position.y+= 1;
    
    if (tiles->position.x > 400) {
    	tiles->position.x = 0;
    	tiles->position.y = 0;    	
    }
    return 1;
}
