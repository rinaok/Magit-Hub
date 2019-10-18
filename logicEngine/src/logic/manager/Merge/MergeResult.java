package logic.manager.Merge;

public enum MergeResult {
    REGULAR_MERGE,
    FAST_FORWARD_MERGE_CONCEALED,
    FAST_FORWARD_MERGE_CONTAINED,
    FAILURE,
    OPEN_CHANGES
}
