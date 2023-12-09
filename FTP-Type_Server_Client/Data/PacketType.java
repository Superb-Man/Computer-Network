package Data;

public enum PacketType {
    ConnectionRequest,
    BooleanResponse,
    ListOfUserRequest,
    ListResponse,
    ListOfOwnfilesRequest,
    ListOfUserFilesRequest,
    FileRequest,
    UnreadMessageRequest,
    FileTransferRequest,
    FileTransferResponse,
    FileChunk,
    FileChunkAcknowledgement,
    FileTrasferComplete,
    TransferCompleteResponse,
    TransferTimeout,
    DownloadRequest
}
