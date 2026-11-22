package edu.ucc.farmsim.domain.progress;

/**
 * What the achievements look at: a plain copy of the counters and of the figures of the game, so the tracker
 * never needs the game state itself (no cycle between {@code game} and {@code progress}, C-01).
 *
 * <p>Stories: HU-26.
 *
 * @param money          money the player has
 * @param harvests       crops harvested so far (clearing a withered plot does not count)
 * @param unitsSold      units sold so far
 * @param buildingsBuilt buildings built so far
 * @param animals        animals living on the farm now
 * @param animalsCured   animals cured so far
 * @param year           current year
 */
public record ProgressFacts(int money, int harvests, int unitsSold, int buildingsBuilt, int animals,
                            int animalsCured, int year) { }
