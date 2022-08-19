package eu.kanade.presentation.more.settings.database

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.domain.source.model.SourceWithCount
import eu.kanade.tachiyomi.ui.setting.database.ClearDatabasePresenter

@Stable
interface ClearDatabaseState {
    val items: List<SourceWithCount>
    val selection: List<Long>
    val isEmpty: Boolean
    var dialog: ClearDatabasePresenter.Dialog?
}

fun ClearDatabaseState(): ClearDatabaseState {
    return ClearDatabaseStateImpl()
}

class ClearDatabaseStateImpl : ClearDatabaseState {
    override var items: List<SourceWithCount> by mutableStateOf(emptyList())
    override var selection: List<Long> by mutableStateOf(emptyList())
    override val isEmpty: Boolean by derivedStateOf { items.isEmpty() }
    override var dialog: ClearDatabasePresenter.Dialog? by mutableStateOf(null)
}
