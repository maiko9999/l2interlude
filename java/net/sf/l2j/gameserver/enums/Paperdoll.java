package net.sf.l2j.gameserver.enums;

public enum Paperdoll
{
    NULL(-1),
    UNDER(0),
    LEAR(1),
    REAR(2),
    NECK(3),
    LFINGER(4),
    RFINGER(5),
    HEAD(6),
    RHAND(7),
    LHAND(8),
    LRHAND(9),  // Adicionado aqui
    GLOVES(10),
    CHEST(11),
    LEGS(12),
    FEET(13),
    CLOAK(14),
    FACE(15),
    HAIR(16),
    HAIRALL(17);

    public static final Paperdoll[] VALUES = values();
    public static final int TOTAL_SLOTS = 18;  // Atualizar para refletir o novo total

    private final int _id;

    private Paperdoll(int id)
    {
        _id = id;
    }

    public int getId()
    {
        return _id;
    }

    public static Paperdoll getEnumByName(String name)
    {
        for (Paperdoll paperdoll : VALUES)
        {
            if (paperdoll.toString().equalsIgnoreCase(name))
                return paperdoll;
        }
        return NULL;
    }

    public static Paperdoll getEnumById(int id)
    {
        for (Paperdoll paperdoll : VALUES)
        {
            if (paperdoll.getId() == id)
                return paperdoll;
        }
        return NULL;
    }
}