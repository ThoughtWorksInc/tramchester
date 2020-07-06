package com.tramchester.geo;

public class GridPosition implements HasGridPosition {

    private final long eastings;
    private final long northings;

    public GridPosition(long eastings, long northings) {

        this.eastings = eastings;
        this.northings = northings;
    }

    public long getEastings() {
        return eastings;
    }

    public long getNorthings() {
        return northings;
    }

    public static boolean withinDistEasting(HasGridPosition gridPositionA, HasGridPosition gridPositionB, long rangeInMeters) {
        return rangeInMeters >= getDistEasting(gridPositionA, gridPositionB) ;
    }

    public static boolean withinDistNorthing(HasGridPosition gridPositionA, HasGridPosition gridPositionB, long rangeInMeters) {
        return rangeInMeters >= getDistNorthing(gridPositionA, gridPositionB);
    }

    private static long getDistNorthing(HasGridPosition gridPositionA, HasGridPosition gridPositionB) {
        return Math.abs(gridPositionA.getNorthings() - gridPositionB.getNorthings());
    }

    private static long getDistEasting(HasGridPosition gridPositionA, HasGridPosition gridPositionB) {
        return Math.abs(gridPositionA.getEastings() - gridPositionB.getEastings());
    }

    private static long getSumSquaresDistance(HasGridPosition gridPositionA, HasGridPosition gridPositionB) {
        long distHorz = getDistEasting(gridPositionA, gridPositionB);
        long distVert = getDistNorthing(gridPositionA, gridPositionB);
        return (distHorz * distHorz) + (distVert * distVert);
    }

    public static boolean withinDist(HasGridPosition gridPositionA, HasGridPosition gridPositionB, long rangeInMeters) {
        long hypSquared = rangeInMeters*rangeInMeters;
        long sum = getSumSquaresDistance(gridPositionA, gridPositionB);
        return sum<=hypSquared;
    }

    public static long distanceTo(HasGridPosition gridPositionA, HasGridPosition gridPositionB) {
        long sum = getSumSquaresDistance(gridPositionA, gridPositionB);
        return Math.round(Math.sqrt(sum));
    }

    @Override
    public String toString() {
        return "GridPosition{" +
                "easting=" + eastings +
                ", northing=" + northings +
                '}';
    }
}
