package igrek.songbook.service.songtree;

import org.junit.Test;
import org.mockito.Mockito;

import igrek.songbook.persistence.songsdb.SongCategoryType;
import igrek.songbook.songselection.SongTreeItem;
import igrek.songbook.songselection.songtree.SongTreeFilter;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SongTreeFilterTest {
	
	@Test
	public void test_matchesNameFilter() {
		
		SongTreeItem songItem = mock(SongTreeItem.class, Mockito.RETURNS_DEEP_STUBS);
		
		when(songItem.getSong().getCategory().getType()).thenReturn(SongCategoryType.ARTIST);
		when(songItem.getSong().getCategory().getName()).thenReturn("Budka suflera");
		when(songItem.getSong().getTitle()).thenReturn("Jolka jolka ążśźęćół ĄĄŻŚŹĘĆ Żółć Łódź");
		// test mockito
		assertThat(songItem.getSong().getCategory().getName()).isEqualTo("Budka suflera");
		
		assertThat(songItem.getSong()
				.displayName()).isEqualTo("Jolka jolka ążśźęćół ĄĄŻŚŹĘĆ Żółć Łódź - Budka suflera");
		
		assertThat(new SongTreeFilter("Budka").songMatchesNameFilter(songItem)).isTrue();
		assertThat(new SongTreeFilter("budka").songMatchesNameFilter(songItem)).isTrue();
		assertThat(new SongTreeFilter("uFL udK").songMatchesNameFilter(songItem)).isTrue();
		assertThat(new SongTreeFilter("jolka suflera").songMatchesNameFilter(songItem)).isTrue();
		assertThat(new SongTreeFilter("dupka").songMatchesNameFilter(songItem)).isFalse();
		assertThat(new SongTreeFilter("dupka suflera").songMatchesNameFilter(songItem)).isFalse();
		// polish letters
		assertThat(new SongTreeFilter("żółć łÓDŹ").songMatchesNameFilter(songItem)).isTrue();
		assertThat(new SongTreeFilter("zolc").songMatchesNameFilter(songItem)).isTrue();
		assertThat(new SongTreeFilter("azszecol aazszec lodz zolc").songMatchesNameFilter(songItem))
				.isTrue();
		
	}
}
