package com.swiftfaze.veil.entities.player;

public class Level {
    private int currentLevel;
    private double xp;

    public Level() {
        currentLevel = 0;
        xp = 0;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public double getXp() {
        return xp;
    }

    public void setXp(double xp) {
        this.xp = xp;
    }

    public int getMaxXp() {
        return (currentLevel + 20) * 5 ;
    }

}
