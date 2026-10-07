package com.dot.gallery.feature.editor.adjustments.filters

import androidx.annotation.Keep
import com.dot.gallery.feature.editor.model.ImageFilter
import kotlinx.serialization.Serializable



@Keep
@Serializable
enum class ImageFilterTypes {
    Original,
    Lite, Playa, Honey, Isla, Desert, Clay, Palma, Blush, Alpaca, Modena,
    West, Metro, Reel, Bazaar, Ollie,
    Onyx, Eiffel, Vogue, Vista, Astro,
    Negative;

    fun createImageFilter(): ImageFilter =
        when (this) {
            Original -> None()
            Negative -> Negative()
            Lite -> LiteFilter()
            Playa -> PlayaFilter()
            Honey -> HoneyFilter()
            Isla -> IslaFilter()
            Desert -> DesertFilter()
            Clay -> ClayFilter()
            Palma -> PalmaFilter()
            Blush -> BlushFilter()
            Alpaca -> AlpacaFilter()
            Modena -> ModenaFilter()
            West -> WestFilter()
            Metro -> MetroFilter()
            Reel -> ReelFilter()
            Bazaar -> BazaarFilter()
            Ollie -> OllieFilter()
            Onyx -> OnyxFilter()
            Eiffel -> EiffelFilter()
            Vogue -> VogueFilter()
            Vista -> VistaFilter()
            Astro -> AstroFilter()
        }

    val filterGroup: Int
        get() = when (this) {
            Original, Lite, Playa, Honey, Isla, Desert, Clay, Palma, Blush, Alpaca, Modena -> 0
            West, Metro, Reel, Bazaar, Ollie -> 1
            Onyx, Eiffel, Vogue, Vista, Astro, Negative -> 2
        }
}
