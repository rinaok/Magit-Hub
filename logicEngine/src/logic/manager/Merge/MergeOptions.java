package logic.manager.Merge;
import java.util.BitSet;
import java.util.EnumSet;

public enum MergeOptions {
    NO_CHANGES(0),
    EXISTS_ONLY_IN_OURS(1),
    EXISTS_ONLY_IN_THEIRS(2),
    ADDED_AND_EDITED_IN_BOTH_SIDES(3),
    EXISTS_ONLY_IN_ANCESTOR(4),
    EXISTS_ONLY_IN_ANCESTOR_AND_OURS_AND_NOT_EQUAL(5),
    EXISTS_ONLY_IN_ANCESTOR_AND_EDITED_IN_THEIRS(6),
    EXISTS_IN_ALL_BUT_NOT_EQUAL_OURS_AND_THEIRS_AND_ANCESTOR(7),
    INVALID8(8),
    INVALID9(9),
    INVALID10(10),
    INVALID11(11),
    INVALID12(12),
    EXISTS_ONLY_IN_OURS_AND_ANCESTOR(13),
    INVALID14(14),
    EDITED_ONLY_ON_THEIRS_AND_EXISTS_IN_ALL(15),
    INVALID16(16),
    INVALID17(17),
    INVALID18(18),
    INVALID19(19),
    INVALID20(20),
    INVALID21(21),
    DELETED_FROM_OURS(22),
    EDITED_ONLY_ON_OURS_AND_EXISTS_IN_ALL(23),
    INVALID24(24),
    INVALID25(25),
    INVALID26(26),
    INVALID27(27),
    INVALID28(28),
    INVALID29(29),
    INVALID30(30),
    INVALID31(31),
    INVALID32(32),
    INVALID33(33),
    INVALID34(34),
    ADDED_AND_EQUAL_IN_OURS_AND_THEIRS(35),
    INVALID36(36),
    INVALID37(37),
    INVALID38(38),
    EXISTS_IN_ALL_AND_EQUAL_IN_OURS_AND_THEIRS(39),
    INVALID40(40),
    INVALID41(41),
    INVALID42(42),
    INVALID43(43),
    INVALID44(44),
    INVALID45(45),
    INVALID46(46),
    INVALID47(47),
    INVALID48(48),
    INVALID49(49),
    INVALID50(50),
    INVALID51(51),
    INVALID52(52),
    INVALID53(53),
    INVALID54(54),
    INVALID55(55),
    INVALID56(56),
    INVALID57(57),
    INVALID58(58),
    INVALID59(59),
    INVALID60(60),
    INVALID61(61),
    INVALID62(62),
    EXISTS_AND_EQUAL_IN_ALL(63),
    CONFLICT_SOLVED(64);

    private final long mergeEnum;

    public static final EnumSet<MergeOptions> CONFLICTS = EnumSet.of(
            ADDED_AND_EDITED_IN_BOTH_SIDES,
            EXISTS_ONLY_IN_ANCESTOR,
            EXISTS_ONLY_IN_ANCESTOR_AND_OURS_AND_NOT_EQUAL,
            EXISTS_ONLY_IN_ANCESTOR_AND_EDITED_IN_THEIRS,
            EXISTS_IN_ALL_BUT_NOT_EQUAL_OURS_AND_THEIRS_AND_ANCESTOR);

    public static final EnumSet<MergeOptions> OPEN_CHANGES = EnumSet.of(
            EXISTS_ONLY_IN_OURS,
            EXISTS_ONLY_IN_THEIRS,
            EDITED_ONLY_ON_THEIRS_AND_EXISTS_IN_ALL,
            EDITED_ONLY_ON_OURS_AND_EXISTS_IN_ALL,
            ADDED_AND_EQUAL_IN_OURS_AND_THEIRS,
            EXISTS_IN_ALL_AND_EQUAL_IN_OURS_AND_THEIRS,
            EXISTS_ONLY_IN_OURS_AND_ANCESTOR,
            DELETED_FROM_OURS,
            CONFLICT_SOLVED);

    public static final EnumSet<MergeOptions> ADD_FILE = EnumSet.of(
            EXISTS_ONLY_IN_OURS,
            EXISTS_ONLY_IN_THEIRS);

    public static final EnumSet<MergeOptions> DELETE_FILE = EnumSet.of(
            EXISTS_ONLY_IN_ANCESTOR,
            EXISTS_ONLY_IN_OURS_AND_ANCESTOR,
            DELETED_FROM_OURS
    );

    MergeOptions(long mergeEnum) {
        this.mergeEnum = mergeEnum;
    }

    public static long convert(BitSet bits) {
        long value = 0L;
        for (int i = 0; i < bits.length(); ++i) {
            value += bits.get(i) ? (1L << i) : 0L;
        }
        return value;
    }
}
