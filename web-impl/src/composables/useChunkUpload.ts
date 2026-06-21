import { ref } from 'vue'
import CryptoJS from 'crypto-js'
import { useBaseStore } from '@/stores/_base'

export interface ChunkUploadOptions {
  file: File
  type?: 'drawing' | 'attachment' | 'report'
  onProgress?: (pct: number) => void
}

const CHUNK_SIZE = 5 * 1024 * 1024

/**
 * 分片上传（Spec 附录 B.3）
 */
export function useChunkUpload() {
  const uploading = ref(false)
  const progress = ref(0)
  const error = ref<string | null>(null)
  const api = useBaseStore().api

  async function computeMd5(file: File): Promise<string> {
    const buffer = await file.arrayBuffer()
    const wordArray = CryptoJS.lib.WordArray.create(new Uint8Array(buffer) as unknown as number[])
    return CryptoJS.MD5(wordArray).toString()
  }

  async function upload(options: ChunkUploadOptions): Promise<{ fileUrl?: string }> {
    uploading.value = true
    progress.value = 0
    error.value = null
    try {
      const md5 = await computeMd5(options.file)
      const initBody = await api.post('/files/init', {
        fileName: options.file.name,
        fileSize: options.file.size,
        md5,
        type: options.type ?? 'attachment',
      })
      const uploadId = initBody?.data?.uploadId ?? initBody?.uploadId
      if (!uploadId) throw new Error('uploadId missing')

      const totalChunks = Math.ceil(options.file.size / CHUNK_SIZE)
      for (let i = 0; i < totalChunks; i++) {
        const start = i * CHUNK_SIZE
        const chunk = options.file.slice(start, start + CHUNK_SIZE)
        const form = new FormData()
        form.append('chunk', chunk)
        await api.post(`/files/chunk?uploadId=${uploadId}&chunk=${i}`, form, {
          headers: { 'Content-Type': 'multipart/form-data' },
        })
        progress.value = Math.round(((i + 1) / totalChunks) * 100)
        options.onProgress?.(progress.value)
      }

      const completeBody = await api.post('/files/complete', { uploadId, md5 })
      return completeBody?.data ?? completeBody
    } catch (e: unknown) {
      error.value = (e as { message?: string })?.message || '上传失败'
      throw e
    } finally {
      uploading.value = false
    }
  }

  return { uploading, progress, error, upload }
}
