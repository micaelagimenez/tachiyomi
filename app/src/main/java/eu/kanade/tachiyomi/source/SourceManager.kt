package eu.kanade.tachiyomi.source

import android.content.Context
import eu.kanade.domain.source.model.SourceData
import eu.kanade.domain.source.repository.SourceDataRepository
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import rx.Observable

class SourceManager(
    private val context: Context,
    private val extensionManager: ExtensionManager,
    private val sourceRepository: SourceDataRepository,
) {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private var sourcesMap = emptyMap<Long, Source>()
        set(value) {
            field = value
            sourcesMapFlow.value = field
        }

    private val sourcesMapFlow = MutableStateFlow(sourcesMap)

    private val stubSourcesMap = mutableMapOf<Long, StubSource>()

    val catalogueSources: Flow<List<CatalogueSource>> = sourcesMapFlow.map { it.values.filterIsInstance<CatalogueSource>() }
    val onlineSources: Flow<List<HttpSource>> = catalogueSources.map { sources -> sources.filterIsInstance<HttpSource>() }

    init {
        scope.launch {
            extensionManager.getInstalledExtensionsFlow()
                .collectLatest { extensions ->
                    val mutableMap = mutableMapOf<Long, Source>(LocalSource.ID to LocalSource(context))
                    extensions.forEach { extension ->
                        extension.sources.forEach {
                            mutableMap[it.id] = it
                            registerStubSource(it.toSourceData())
                        }
                    }
                    sourcesMap = mutableMap
                }
        }

        scope.launch {
            sourceRepository.subscribeAll()
                .collectLatest { sources ->
                    val mutableMap = stubSourcesMap.toMutableMap()
                    sources.forEach {
                        mutableMap[it.id] = StubSource(it)
                    }
                }
        }
    }

    fun get(sourceKey: Long): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOrStub(sourceKey: Long): Source {
        return sourcesMap[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { createStubSource(sourceKey) }
        }
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance<HttpSource>()

    fun getCatalogueSources() = sourcesMap.values.filterIsInstance<CatalogueSource>()

    fun getStubSources(): List<StubSource> {
        val onlineSourceIds = getOnlineSources().map { it.id }
        return stubSourcesMap.values.filterNot { it.id in onlineSourceIds }
    }

    private fun registerStubSource(sourceData: SourceData) {
        scope.launch {
            val (id, lang, name) = sourceData
            sourceRepository.upsertSourceData(id, lang, name)
        }
    }

    private suspend fun createStubSource(id: Long): StubSource {
        sourceRepository.getSourceData(id)?.let {
            return StubSource(it)
        }
        extensionManager.getSourceData(id)?.let {
            registerStubSource(it)
            return StubSource(it)
        }
        return StubSource(SourceData(id, "", ""))
    }

    @Suppress("OverridingDeprecatedMember")
    open inner class StubSource(val sourceData: SourceData) : Source {

        override val id: Long = sourceData.id

        override val name: String = sourceData.name.ifBlank { id.toString() }

        override val lang: String = sourceData.lang

        override suspend fun getMangaDetails(manga: SManga): SManga {
            throw getSourceNotInstalledException()
        }

        override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
            return Observable.error(getSourceNotInstalledException())
        }

        override suspend fun getChapterList(manga: SManga): List<SChapter> {
            throw getSourceNotInstalledException()
        }

        override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
            return Observable.error(getSourceNotInstalledException())
        }

        override suspend fun getPageList(chapter: SChapter): List<Page> {
            throw getSourceNotInstalledException()
        }

        override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
            return Observable.error(getSourceNotInstalledException())
        }

        override fun toString(): String {
            return if (sourceData.isMissingInfo.not()) "$name (${lang.uppercase()})" else id.toString()
        }

        fun getSourceNotInstalledException(): SourceNotInstalledException {
            return SourceNotInstalledException(toString())
        }
    }

    inner class SourceNotInstalledException(val sourceString: String) :
        Exception(context.getString(R.string.source_not_installed, sourceString))
}
