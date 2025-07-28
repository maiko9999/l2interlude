package ext.mods.tour.ranking.holder;

public class PlayerRankingData
{
    private final String _playerName;
    private int _wins;
    private int _losses;
    private int _draws;

    public PlayerRankingData(String playerName)
    {
        _playerName = playerName;
    }

    public void addWin()
    {
        _wins++;
    }

    public void addLoss()
    {
        _losses++;
    }

    public void addDraw()
    {
        _draws++;
    }

    public int getWins()
    {
        return _wins;
    }

    public int getLosses()
    {
        return _losses;
    }

    public int getDraws()
    {
        return _draws;
    }

    public int getPoints()
    {
        return (_wins * 3) + (_draws * 1); // 3 pontos por vitória, 1 ponto por empate
    }

    public String getPlayerName()
    {
        return _playerName;
    }
}
