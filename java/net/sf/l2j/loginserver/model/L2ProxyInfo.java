package net.sf.l2j.loginserver.model;

public record L2ProxyInfo(int gamesServerId, boolean hidesGameServer, boolean fallbackToGameServer)
{
}