package com.yinqi.player.player

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionLayoutTest {
    @Test
    fun togglesBetweenGridAndList() {
        assertEquals(CollectionLayout.List, CollectionLayout.Grid.next())
        assertEquals(CollectionLayout.Grid, CollectionLayout.List.next())
    }
}
