package eu.kanade.tachiyomi.ui.browse.source

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.ui.base.presenter.BasePresenter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

/**
 * Presenter of [SourceController]
 * Function calls should be done from here. UI calls should be done from the controller.
 */
class SourcePresenter(
    val sourceManager: SourceManager = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
) : BasePresenter<SourceController>() {

    var sources = getEnabledSources()

    /**
     * Unsubscribe and create a new subscription to fetch enabled sources.
     */
    private fun loadSources() {
        val pinnedSources = mutableListOf<SourceItem>()
        val pinnedSourceIds = preferences.pinnedSources().get()

        val map = TreeMap<String, MutableList<CatalogueSource>> { d1, d2 ->
            // Catalogues without a lang defined will be placed at the end
            when {
                d1 == "" && d2 != "" -> 1
                d2 == "" && d1 != "" -> -1
                else -> d1.compareTo(d2)
            }
        }
        val byLang = sources.groupByTo(map) { it.lang }
        var sourceItems = byLang.flatMap {
            val langItem = LangItem(it.key)
            it.value.map { source ->
                val isPinned = source.id.toString() in pinnedSourceIds
                if (isPinned) {
                    pinnedSources.add(SourceItem(source, LangItem(PINNED_KEY), isPinned))
                }

                SourceItem(source, langItem, isPinned)
            }
        }

        if (pinnedSources.isNotEmpty()) {
            sourceItems = pinnedSources + sourceItems
        }

        view?.setSources(sourceItems)
    }

    private fun loadLastUsedSource() {
        // Immediate initial load
        preferences.lastUsedSource().get().let { updateLastUsedSource(it) }

        // Subsequent updates
        preferences.lastUsedSource().asFlow()
            .drop(1)
            .onStart { delay(500) }
            .distinctUntilChanged()
            .onEach { updateLastUsedSource(it) }
            .launchIn(presenterScope)
    }

    private fun updateLastUsedSource(sourceId: Long) {
        val source = (sourceManager.get(sourceId) as? CatalogueSource)?.let {
            val isPinned = it.id.toString() in preferences.pinnedSources().get()
            SourceItem(it, null, isPinned)
        }
        source?.let { view?.setLastUsedSource(it) }
    }

    fun updateSources() {
        sources = getEnabledSources()
        loadSources()
        loadLastUsedSource()
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     *
     * @return list containing enabled sources.
     */
    private fun getEnabledSources(): List<CatalogueSource> {
        val languages = preferences.enabledLanguages().get()
        val disabledSourceIds = preferences.disabledSources().get()

        return sourceManager.getCatalogueSources()
            .filter { it.lang in languages || it.id == LocalSource.ID }
            .filterNot { it.id.toString() in disabledSourceIds }
            .sortedBy { "(${it.lang}) ${it.name.lowercase()}" }
    }

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"
    }
}
