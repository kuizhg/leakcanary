package com.example.leakcanary;

import leakcanary.LeakCanary;

public class JavaUsage {
  static void replaceConfig() {
    LeakCanary.Config config = LeakCanary.getConfig().newBuilder()
        .dumpHeap(false)
        .retainedVisibleThreshold(3)
        .maxStoredHeapDumps(10)
        .build();
    LeakCanary.setConfig(config);
  }
}
