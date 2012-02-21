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
#pragma rs java_package_name(de.devisnik.rs.slider)


#include "rs_graphics.rsh"

#pragma stateVertex(parent)
#pragma stateStore(parent)

typedef struct __attribute__((packed, aligned(4))) Tile {
    float2 position;
    float2 destination;
    int steps;
    int hole;
    rs_allocation texture;
} Tile_t;
Tile_t *tiles;

int gSolving;
rs_program_fragment gProgramFragment;


// This is invoked automatically when the script is created
void init() {
}


static void renderTile(uint32_t index) {
	Tile_t *tile = &tiles[index];
	if (tile->steps > 0) {
		// (float) (Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
		//-cos(angle * M_PI / 180) * RADIUS;
		//tile->position.x += (tile->destination.x - tile->position.x) / 50.f * ((cos((tile->steps/50.f+1)*M_PI) /2.f)+.5f);
		//tile->position.y += (tile->destination.y - tile->position.y) / 50.f * ((cos((tile->steps/50.f+1)*M_PI) /2.f)+.5f);
		tile->position.x += (tile->destination.x - tile->position.x) / tile->steps;
		tile->position.y += (tile->destination.y - tile->position.y) / tile->steps;
		tile->steps--;
	}
//	rsDebug("steps", tile->steps);
	if (tile->hole && gSolving)
		return;
	rsgBindTexture(gProgramFragment, 0, tile->texture);
	rsgDrawRect(tile->position.x, tile->position.y, tile->position.x+1, tile->position.y+1, 0);	
}

static void updatePosition(uint32_t index) {
	Tile_t tile = tiles[index];
}

static int frames=0;
static int64_t laptime = 0;
static void measureFPS() {
    frames++;
    int64_t now = rsUptimeMillis();
    if (now - laptime >= 1000) {
		rsDebug("fps:", frames);
		frames = 0;
	    laptime = now;
    }
}

int root() {

    // Clear the background color
    rsgClearColor(0.f, 0.f, 0.0f, 0.0f);

	rsgBindProgramFragment(gProgramFragment);
	
//    rs_matrix4x4 matrix;
//	rsMatrixRotate(&matrix, 50.f, 1.0f, 1.0f, 1.0f);
//    rsgProgramVertexLoadModelMatrix(&matrix);	

    uint32_t dimX = rsAllocationGetDimX(rsGetAllocation(tiles));
    for (uint32_t ct=0; ct < dimX; ct++) {
		renderTile(ct);
    }
    measureFPS();
    return 10;
}
