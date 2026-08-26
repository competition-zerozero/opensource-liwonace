package zerozero.opensource.dto;

public record DocumentSearchResult(
        long id,
        String docId,
        int chunkIndex,
        String content,
        String metadata,
        double distance
) {
}
