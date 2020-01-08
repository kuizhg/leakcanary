package leakcanary

import shark.AndroidMetadataExtractor
import shark.AndroidObjectInspectors
import shark.AndroidReferenceMatchers
import shark.FilteringLeakingObjectFinder
import shark.HeapAnalysisSuccess
import shark.IgnoredReferenceMatcher
import shark.KeyedWeakReferenceFinder
import shark.LeakingObjectFinder
import shark.LibraryLeakReferenceMatcher
import shark.MetadataExtractor
import shark.ObjectInspector
import shark.ReferenceMatcher

class ConfigBuilder(config: LeakCanary.Config) {
  private var dumpHeap: Boolean = config.dumpHeap
  private var dumpHeapWhenDebugging: Boolean = config.dumpHeapWhenDebugging
  private var retainedVisibleThreshold: Int = config.retainedVisibleThreshold
  private var referenceMatchers: List<ReferenceMatcher> = config.referenceMatchers
  private var objectInspectors: List<ObjectInspector> = config.objectInspectors
  private var onHeapAnalyzedListener: OnHeapAnalyzedListener = config.onHeapAnalyzedListener
  private var metatadaExtractor: MetadataExtractor = config.metatadaExtractor
  private var computeRetainedHeapSize: Boolean = config.computeRetainedHeapSize
  private var maxStoredHeapDumps: Int = config.maxStoredHeapDumps
  private var requestWriteExternalStoragePermission: Boolean = config.requestWriteExternalStoragePermission
  private var leakingObjectFinder: LeakingObjectFinder = config.leakingObjectFinder
  private var useExperimentalLeakFinders: Boolean = config.useExperimentalLeakFinders


  /**
   * Whether LeakCanary should dump the heap when enough retained instances are found. This needs
   * to be true for LeakCanary to work, but sometimes you may want to temporarily disable
   * LeakCanary (e.g. for a product demo).
   *
   * Defaults to true.
   */
  fun dumpHeap(value: Boolean) = apply { dumpHeap = value }

  /**
   * If [dumpHeapWhenDebugging] is false then LeakCanary will not dump the heap
   * when the debugger is attached. The debugger can create temporary memory leaks (for instance
   * if a thread is blocked on a breakpoint).
   *
   * Defaults to false.
   */
  fun dumpHeapWhenDebugging(value: Boolean) = apply { dumpHeapWhenDebugging = value }

  /**
   * When the app is visible, LeakCanary will wait for at least
   * [retainedVisibleThreshold] retained instances before dumping the heap. Dumping the heap
   * freezes the UI and can be frustrating for developers who are trying to work. This is
   * especially frustrating as the Android Framework has a number of leaks that cannot easily
   * be fixed.
   *
   * When the app becomes invisible, LeakCanary dumps the heap after
   * [AppWatcher.Config.watchDurationMillis] ms.
   *
   * The app is considered visible if it has at least one activity in started state.
   *
   * A higher threshold means LeakCanary will dump the heap less often, therefore it won't be
   * bothering developers as much but it could miss some leaks.
   *
   * Defaults to 5.
   */
  fun retainedVisibleThreshold(value: Int) = apply { retainedVisibleThreshold = value }

  /**
   * Known patterns of references in the heap, lister here either to ignore them
   * ([IgnoredReferenceMatcher]) or to mark them as library leaks ([LibraryLeakReferenceMatcher]).
   *
   * When adding your own custom [LibraryLeakReferenceMatcher] instances, you'll most
   * likely want to set [LibraryLeakReferenceMatcher.patternApplies] with a filter that checks
   * for the Android OS version and manufacturer. The build information can be obtained by calling
   * [shark.AndroidBuildMirror.fromHeapGraph].
   *
   * Defaults to [AndroidReferenceMatchers.appDefaults]
   */
  fun referenceMatchers(value: List<ReferenceMatcher>) = apply { referenceMatchers = value }

  /**
   * List of [ObjectInspector] that provide LeakCanary with insights about objects found in the
   * heap. You can create your own [ObjectInspector] implementations, and also add
   * a [shark.AppSingletonInspector] instance created with the list of internal singletons.
   *
   * Defaults to [AndroidObjectInspectors.appDefaults]
   */
  fun objectInspectors(value: List<ObjectInspector>) = apply { objectInspectors = value }

  /**
   * Called on a background thread when the heap analysis is complete.
   * If you want leaks to be added to the activity that lists leaks, make sure to delegate
   * calls to a [DefaultOnHeapAnalyzedListener].
   *
   * Defaults to [DefaultOnHeapAnalyzedListener]
   */
  fun onHeapAnalyzedListener(value: OnHeapAnalyzedListener) = apply { onHeapAnalyzedListener = value }

  /**
   * Extracts metadata from a hprof to be reported in [HeapAnalysisSuccess.metadata].
   * Called on a background thread during heap analysis.
   *
   * Defaults to [AndroidMetadataExtractor]
   */
  fun metatadaExtractor(value: MetadataExtractor) = apply { metatadaExtractor = value }

  /**
   * Whether to compute the retained heap size, which is the total number of bytes in memory that
   * would be reclaimed if the detected leaks didn't happen. This includes native memory
   * associated to Java objects (e.g. Android bitmaps).
   *
   * Computing the retained heap size can slow down the analysis because it requires navigating
   * from GC roots through the entire object graph, whereas [shark.HeapAnalyzer] would otherwise
   * stop as soon as all leaking instances are found.
   *
   * Defaults to true.
   */
  fun computeRetainedHeapSize(value: Boolean) = apply { computeRetainedHeapSize = value }

  /**
   * How many heap dumps are kept on the Android device for this app package. When this threshold
   * is reached LeakCanary deletes the older heap dumps. As several heap dumps may be enqueued
   * you should avoid going down to 1 or 2.
   *
   * Defaults to 7.
   */
  fun maxStoredHeapDumps(value: Int) = apply { maxStoredHeapDumps = value }

  /**
   * LeakCanary always attempts to store heap dumps on the external storage if the
   * WRITE_EXTERNAL_STORAGE is already granted, and otherwise uses the app storage.
   * If the WRITE_EXTERNAL_STORAGE permission is not granted and
   * [requestWriteExternalStoragePermission] is true, then LeakCanary will display a notification
   * to ask for that permission.
   *
   * Defaults to false because that permission notification can be annoying.
   */
  fun requestWriteExternalStoragePermission(value: Boolean) = apply { requestWriteExternalStoragePermission = value }

  /**
   * Finds the objects that are leaking, for which LeakCanary will compute leak traces.
   *
   * Defaults to [KeyedWeakReferenceFinder] which finds all objects tracked by a
   * [KeyedWeakReference], ie all objects that were passed to [ObjectWatcher.watch].
   *
   * You could instead replace it with a [FilteringLeakingObjectFinder], which scans all objects
   * in the heap dump and delegates the decision to a list of
   * [FilteringLeakingObjectFinder.LeakingObjectFilter]. This can lead to finding more leaks
   * than the default and shorter leak traces. This also means that every analysis during a
   * given process life will bring up the same leaking objects over and over again, unlike
   * when using [KeyedWeakReferenceFinder] (because [KeyedWeakReference] instances are cleared
   * after each heap dump).
   *
   * The list of filters can be built from [AndroidObjectInspectors]:
   *
   * ```
   * LeakCanary.config = LeakCanary.config.copy(
   *     leakingObjectFinder = FilteringLeakingObjectFinder(
   *         AndroidObjectInspectors.appLeakingObjectFilters
   *     )
   * )
   * ```
   */
  fun leakingObjectFinder(value: LeakingObjectFinder) = apply { leakingObjectFinder = value }

  /**
   * Deprecated: This is a no-op, set a custom [leakingObjectFinder] instead.
   */
  @Deprecated("This is a no-op, set a custom leakingObjectFinder instead")
  fun useExperimentalLeakFinders(value: Boolean) = apply { useExperimentalLeakFinders = value }

  fun build(): LeakCanary.Config =
    LeakCanary.config.copy(
        dumpHeap = this.dumpHeap,
        dumpHeapWhenDebugging = this.dumpHeapWhenDebugging,
        retainedVisibleThreshold = this.retainedVisibleThreshold,
        referenceMatchers = this.referenceMatchers,
        objectInspectors = this.objectInspectors,
        onHeapAnalyzedListener = this.onHeapAnalyzedListener,
        metatadaExtractor = this.metatadaExtractor,
        computeRetainedHeapSize = this.computeRetainedHeapSize,
        maxStoredHeapDumps = this.maxStoredHeapDumps,
        requestWriteExternalStoragePermission = this.requestWriteExternalStoragePermission,
        leakingObjectFinder = this.leakingObjectFinder,
        useExperimentalLeakFinders = this.useExperimentalLeakFinders
    )
}