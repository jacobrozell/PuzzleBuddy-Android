package com.jacobrozell.puzzlebuddy.domain.photo

object PuzzlePhotoSemantics {
    enum class MoveDirection {
        EARLIER,
        LATER,
    }

    fun photosInOrder(photos: List<PuzzlePhoto>): List<PuzzlePhoto> =
        photos.sortedBy { it.sortOrder }

    fun cover(photos: List<PuzzlePhoto>): PuzzlePhoto? = photosInOrder(photos).firstOrNull()

    fun normalizedSortOrders(photos: List<PuzzlePhoto>): List<PuzzlePhoto> =
        photos.mapIndexed { index, photo -> photo.copy(sortOrder = index) }

    fun sortedAndNormalized(photos: List<PuzzlePhoto>): List<PuzzlePhoto> =
        normalizedSortOrders(photosInOrder(photos))

    fun movingPhoto(photoId: String, beforeId: String, photos: List<PuzzlePhoto>): List<PuzzlePhoto> {
        val ordered = photosInOrder(photos).toMutableList()
        val fromIndex = ordered.indexOfFirst { it.id == photoId }
        val toIndex = ordered.indexOfFirst { it.id == beforeId }
        if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return photos
        val photo = ordered.removeAt(fromIndex)
        val adjustedIndex = if (toIndex > fromIndex) toIndex - 1 else toIndex
        ordered.add(adjustedIndex, photo)
        return normalizedSortOrders(ordered)
    }

    fun movingPhotoOneStep(photoId: String, direction: MoveDirection, photos: List<PuzzlePhoto>): List<PuzzlePhoto> {
        val ordered = photosInOrder(photos)
        val index = ordered.indexOfFirst { it.id == photoId }
        if (index < 0) return photos
        return when (direction) {
            MoveDirection.EARLIER -> {
                if (index == 0) photos
                else movingPhoto(photoId, ordered[index - 1].id, photos)
            }
            MoveDirection.LATER -> {
                if (index >= ordered.lastIndex) photos
                else if (index + 2 < ordered.size) movingPhoto(photoId, ordered[index + 2].id, photos)
                else movingPhotoToEnd(photoId, photos)
            }
        }
    }

    fun movingPhotoToCover(photoId: String, photos: List<PuzzlePhoto>): List<PuzzlePhoto> {
        val ordered = photosInOrder(photos)
        val first = ordered.firstOrNull() ?: return photos
        if (first.id == photoId) return photos
        return movingPhoto(photoId, first.id, photos)
    }

    fun movingPhotoToEnd(photoId: String, photos: List<PuzzlePhoto>): List<PuzzlePhoto> {
        val ordered = photosInOrder(photos).toMutableList()
        val fromIndex = ordered.indexOfFirst { it.id == photoId }
        if (fromIndex < 0 || fromIndex >= ordered.lastIndex) return photos
        val photo = ordered.removeAt(fromIndex)
        ordered.add(photo)
        return normalizedSortOrders(ordered)
    }
}
