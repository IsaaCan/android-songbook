//Generated by the protocol buffer compiler. DO NOT EDIT!
// source: shared_song.proto

package mwongela.songbook.custom.share.protos

@kotlin.jvm.JvmSynthetic
inline fun sharedSongDto(block: mwongela.songbook.custom.share.protos.SharedSongDtoKt.Dsl.() -> Unit): SharedSong.SharedSongDto =
    mwongela.songbook.custom.share.protos.SharedSongDtoKt.Dsl._create(SharedSong.SharedSongDto.newBuilder())
        .apply { block() }._build()

object SharedSongDtoKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    class Dsl private constructor(
        @kotlin.jvm.JvmField val _builder: SharedSong.SharedSongDto.Builder
    ) {
        companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: SharedSong.SharedSongDto.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): SharedSong.SharedSongDto = _builder.build()

        /**
         * <code>string title = 1;</code>
         */
        var title: kotlin.String
            @JvmName("getTitle")
            get() = _builder.title
            @JvmName("setTitle")
            set(value) {
                _builder.title = value
            }

        /**
         * <code>string title = 1;</code>
         */
        fun clearTitle() {
            _builder.clearTitle()
        }

        /**
         * <code>string content = 2;</code>
         */
        var content: kotlin.String
            @JvmName("getContent")
            get() = _builder.content
            @JvmName("setContent")
            set(value) {
                _builder.content = value
            }

        /**
         * <code>string content = 2;</code>
         */
        fun clearContent() {
            _builder.clearContent()
        }

        /**
         * <code>optional string customCategory = 3;</code>
         */
        var customCategory: kotlin.String
            @JvmName("getCustomCategory")
            get() = _builder.customCategory
            @JvmName("setCustomCategory")
            set(value) {
                _builder.customCategory = value
            }

        /**
         * <code>optional string customCategory = 3;</code>
         */
        fun clearCustomCategory() {
            _builder.clearCustomCategory()
        }

        /**
         * <code>optional string customCategory = 3;</code>
         * @return Whether the customCategory field is set.
         */
        fun hasCustomCategory(): kotlin.Boolean {
            return _builder.hasCustomCategory()
        }

        /**
         * <code>optional int64 chordsNotation = 4;</code>
         */
        var chordsNotation: kotlin.Long
            @JvmName("getChordsNotation")
            get() = _builder.chordsNotation
            @JvmName("setChordsNotation")
            set(value) {
                _builder.chordsNotation = value
            }

        /**
         * <code>optional int64 chordsNotation = 4;</code>
         */
        fun clearChordsNotation() {
            _builder.clearChordsNotation()
        }

        /**
         * <code>optional int64 chordsNotation = 4;</code>
         * @return Whether the chordsNotation field is set.
         */
        fun hasChordsNotation(): kotlin.Boolean {
            return _builder.hasChordsNotation()
        }
    }
}

@kotlin.jvm.JvmSynthetic
inline fun SharedSong.SharedSongDto.copy(block: mwongela.songbook.custom.share.protos.SharedSongDtoKt.Dsl.() -> Unit): SharedSong.SharedSongDto =
    mwongela.songbook.custom.share.protos.SharedSongDtoKt.Dsl._create(this.toBuilder())
        .apply { block() }
        ._build()
