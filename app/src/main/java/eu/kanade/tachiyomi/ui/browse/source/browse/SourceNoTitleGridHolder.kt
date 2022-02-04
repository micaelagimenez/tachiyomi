package eu.kanade.tachiyomi.ui.browse.source.browse

import android.view.View
import androidx.core.view.isVisible
import coil.clear
import coil.imageLoader
import coil.request.ImageRequest
import coil.transition.CrossfadeTransition
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.database.models.Manga
import eu.kanade.tachiyomi.databinding.SourceNoTitleGridItemBinding
import eu.kanade.tachiyomi.widget.StateImageViewTarget

class SourceNoTitleGridHolder(private val view: View, private val adapter: FlexibleAdapter<*>) :
    SourceHolder<SourceNoTitleGridItemBinding>(view, adapter) {

    override val binding = SourceNoTitleGridItemBinding.bind(view)

    /**
     * Method called from [CatalogueAdapter.onBindViewHolder]. It updates the data for this
     * holder with the given manga.
     *
     * @param manga the manga to bind.
     */
    override fun onSetValues(manga: Manga) {
        // Set alpha of thumbnail.
        binding.thumbnail.alpha = if (manga.favorite) 0.3f else 1.0f

        // For rounded corners
        binding.badges.leftBadges.clipToOutline = true
        binding.badges.rightBadges.clipToOutline = true

        // Set favorite badge
        binding.badges.favoriteText.isVisible = manga.favorite

        setImage(manga)
    }

    override fun setImage(manga: Manga) {
        // For rounded corners
        binding.card.clipToOutline = true

        binding.thumbnail.clear()
        if (!manga.thumbnail_url.isNullOrEmpty()) {
            val crossfadeDuration = view.context.imageLoader.defaults.transition.let {
                if (it is CrossfadeTransition) it.durationMillis else 0
            }
            val request = ImageRequest.Builder(view.context)
                .data(manga)
                .setParameter(MangaCoverFetcher.USE_CUSTOM_COVER, false)
                .target(StateImageViewTarget(binding.thumbnail, binding.progress, crossfadeDuration))
                .build()
            itemView.context.imageLoader.enqueue(request)
        } else {
            // Set manga title
            binding.title.text = manga.title
        }
    }
}
