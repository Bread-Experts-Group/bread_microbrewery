package org.bread_experts_group.demo

import org.bread_experts_group.common.Mod

class DemoMod : Mod(
	"beg_bm_demo_mod",
	"BEG-BM Demonstration Mod",
	"1.0.0",
	"Demonstration mod for the BreadExpertsGroup microbrewery loader with interop"
) {
	init {
		logger.info("Demo mod loaded")
	}
}