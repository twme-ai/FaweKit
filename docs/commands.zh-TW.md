# FaweKit 完整指令手冊

本文件以 FaweKit `0.1.0` 的實際實作為準，介紹插件本身的命令、加入
FAWE 的遮罩，以及由 FaweKit 改寫後交給 FAWE 執行的相容語法。

FaweKit 是獨立的社群專案，並非 FastAsyncWorldEdit 官方插件。本文只說明
FaweKit 增加或改寫的功能；FAWE 原生命令、遮罩及圖樣仍應以 FAWE 文件為準。

## 閱讀語法

- `//` 是命令本身的一部分。例如玩家要在聊天欄輸入 `//tpsel`，不是
  `/tpsel`。
- `<參數>` 表示必填，`[參數]` 表示選填，`a|b` 表示二選一。
- `[-abc]` 表示旗標可以分開寫，也可以合併，例如 `-a -b` 與 `-ab`。
- 頁碼是否從 `0` 或 `1` 開始、堆疊索引是否允許負數，會在各命令中明確
  說明，不能互換。
- 除了 `//help-masks`、`//help-patterns` 與 `//echo` 外，FaweKit 命令都必須
  由遊戲內玩家執行。
- 命令中的 `<mask>` 與 `<pattern>` 使用 FAWE 解析器。對 FaweKit 的 Bukkit
  命令而言，每一個遮罩或圖樣必須放在單一、不含空白的參數內。

## 指令速查

| 指令 | 用途 | 權限 |
| --- | --- | --- |
| `//tpsel`, `//seltp` | 傳送到目前選區或選取堆疊中的選區 | `fawekit.tpsel` |
| `//multireplace`, `//multirepl` | 在一次可復原操作中執行多組取代 | `fawekit.multireplace` |
| `//clipboard` | 檢視、縮放、裁切與切換剪貼簿 | `fawekit.clipboard` |
| `//copynear` | 搜尋附近方塊，建立選區並複製 | `fawekit.copynear` |
| `//autorotatepaste`, `//arp` | 依兩次選區方向自動旋轉並貼上 | `fawekit.autorotatepaste` |
| `//msel` | 管理記憶體內的選取堆疊 | `fawekit.msel` |
| `//ssel` | 將選區存成玩家專屬 YAML 檔 | `fawekit.ssel` |
| `//bmask` | 依生態域設定 FAWE 全域遮罩 | `fawekit.bmask` |
| `//help-masks` | 顯示遮罩速查表 | `fawekit.help` |
| `//help-patterns` | 顯示圖樣速查表 | `fawekit.help` |
| `//echo` | 展開 Minecraft 方塊名稱萬用字元並顯示結果 | `fawekit.echo` |
| `//shortcut`, `//sc` | 建立及執行玩家專屬捷徑 | `fawekit.shortcut` |
| `//pin`, `//unpin` | 固定或解除 FAWE 原生命令看到的位置 | `fawekit.pin` |
| `//schematic search` | 模糊排序並列出 schematic 檔案 | `worldedit.schematic.load` |

權限預設值詳見本文件最後的[權限總表](#權限總表)。

## `//tpsel`：傳送到選區

別名：`//seltp`

```text
//tpsel [-s <index>] [<x> <y> <z>]
```

不提供座標時，命令會嘗試尋找安全位置：

1. 最多隨機嘗試 64 次，在選區水平包圍盒外 16 格內尋找露天位置。腳下必須
   是實心方塊，腳部與頭部必須可通過，天空光照必須大於 `1`。
2. 找不到時，再於選區的三維包圍盒內最多嘗試 64 次。除了安全站立與總光照
   大於 `1` 外，目的地周圍 `3 x 2 x 3` 的空間也必須可通過。
3. 仍找不到就取消傳送並顯示錯誤。非方形選區使用的是包圍盒，因此第二階段
   找到的位置不一定落在選區的精確形狀內。

提供三個座標時，座標以選區中心的整數方塊座標為相對基準：

- `~`：該軸中心加 `0.5`，落在方塊中央。
- `~5`、`~-2.5`：中心加 `0.5` 後再加指定偏移量。
- `120`、`64.5`：絕對世界座標。
- 座標模式不做安全空間檢查，並保留玩家原本的視角方向。

`-s <index>` 用來指定 `//msel` 堆疊中的選區：

- `1` 是堆疊頂端，`2` 是下一項。
- `-1` 是堆疊底端，`-2` 是倒數第二項。
- `0` 或省略 `-s` 會使用目前啟用的 FAWE 選區。

範例：

```text
//tpsel
//tpsel ~ ~5 ~
//tpsel -s 2
//tpsel -s -1 ~ ~2 ~
```

## `//multireplace`：多組條件取代

別名：`//multirepl`

```text
//multireplace <mask> <pattern> [<mask> <pattern> ...]
```

參數必須成對出現。命令先解析所有遮罩與圖樣，再逐格檢查目前選區：

- 同一方塊可以同時符合多個遮罩。
- 若符合多組規則，最後一組符合的圖樣獲勝。
- 所有遮罩都針對操作開始時的世界狀態判斷，因此可以安全地交換兩種方塊，
  不會讓第一組取代結果又被第二組當成輸入。
- 所有變更共用一個 EditSession，可用一次 `//undo` 復原。
- 仍會套用玩家的 FAWE 限制與全域遮罩。

範例：

```text
//multireplace dirt stone stone dirt
//multireplace oak_log stripped_oak_log oak_wood stripped_oak_wood
//multireplace #skylight[15] glass #blocklight[1][15] glowstone
```

Minecraft/Bukkit 不保證用引號合併含空白參數；實際輸入時請讓每個
`<mask>` 與 `<pattern>` 本身不含空白。

## `//clipboard`：剪貼簿工具

所有子命令都需要目前 FAWE 工作階段已有剪貼簿。

### 顯示尺寸

```text
//clipboard
//clipboard size
```

兩種寫法效果相同，會顯示剪貼簿的寬、高、長與原點座標。

### 拉伸與壓縮

```text
//clipboard stretch <width> <height> <length>
//clipboard compress <width> <height> <length>
```

`stretch` 與 `compress` 目前使用同一套重採樣邏輯；兩者都能放大或縮小，不會
因子命令名稱限制方向。每個尺寸接受：

| 寫法 | 意義，假設原尺寸為 `20` |
| --- | --- |
| `12` | 新尺寸為 12 |
| `50%` | 新尺寸為原本的 50%，結果為 10 |
| `~5` | 在原尺寸上加 5，結果為 25 |
| `~-5` | 在原尺寸上減 5，結果為 15 |
| `~25%` | 在原尺寸上再加原尺寸的 25%，結果為 25 |
| `~` | 維持原尺寸 |

計算結果會四捨五入為整數，且每一軸最小為 `1`。縮放採最近鄰取樣，不會做
平滑、混合或旋轉。

```text
//clipboard stretch 200% 100% 200%
//clipboard compress 16 8 16
//clipboard stretch ~5 ~ ~25%
```

### 裁切或擴張

```text
//clipboard crop [-w <width>] [-h <height>] [-l <length>]
                 [-x <offset>] [-y <offset>] [-z <offset>]
```

- `-w`、`-h`、`-l` 設定輸出寬、高、長，接受與縮放相同的絕對值、百分比與
  `~` 相對格式。
- `-x`、`-y`、`-z` 是來源起點的整數偏移，不接受百分比或 `~`。
- 正偏移會略過來源低座標側的方塊；負偏移會讓輸出從來源範圍之前開始。
- 輸出超出原剪貼簿的部分保留為空氣，因此這個命令也能用來增加留白。
- 原點會依裁切偏移同步調整。

```text
//clipboard crop -w 20 -h 10 -l 20
//clipboard crop -x 2 -y 1 -z 2 -w ~-4 -h ~-1 -l ~-4
//clipboard crop -x -3 -w ~6
```

`stretch`、`compress` 與 `crop` 都會重建主要剪貼簿。完整方塊資料會保留，但
生態域、實體、多剪貼簿中的其他項目及既有 transform 不會一併複製；需要這些
資料時應先保留原 schematic 或重新執行複製。

### 列出多剪貼簿

```text
//clipboard list [-d] [-n] [-p <page>]
```

- 每頁 10 項。
- 頁碼從 `0` 開始；第一頁是 `-p 0`。
- 每項顯示從 `0` 開始的剪貼簿索引、URI 或替代名稱，以及尺寸。
- `-d` 與 `-n` 是為相容提案語法而接受的顯示旗標，目前不會改變輸出。

### 選擇主要剪貼簿

```text
//clipboard select [-n] <index>
```

索引來自 `//clipboard list`，從 `0` 開始。選定後該項會成為工作階段唯一的主要
ClipboardHolder；`-n` 目前是相容旗標，與不加旗標的行為相同。

```text
//clipboard list -p 0
//clipboard select 2
```

## `//copynear`：搜尋附近方塊並複製

```text
//copynear [-xbce] [-m <copyMask>] <searchMask> [distance]
```

命令以玩家所在方塊為中心，在球形半徑內尋找符合 `<searchMask>` 的位置。預設
距離是 `64`，可用範圍是 `1` 到 `256`。搜尋前會以完整立方體尺寸估算檢查量；
若超過玩家的 FAWE `MAX_CHECKS` 限制，就不會開始搜尋。

找到位置後：

- 1 至 3 個符合點會建立包含它們的長方體選區。
- 4 個以上符合點會建立凸多面體選區。
- 新選區會立即成為目前選區並傳送 CUI 更新。
- 命令把該選區內容複製到記憶體最佳化剪貼簿。凸多面體包圍盒內、選區外的
  位置會維持空氣。
- 完全找不到符合點時不會改變剪貼簿。

旗標：

| 旗標 | 行為 |
| --- | --- |
| `-x` | 搜尋時仍以符合點建立選區，但不把符合 `<searchMask>` 的方塊寫入剪貼簿 |
| `-b` | 一併複製生態域 |
| `-c` | 將原點設為選區 X/Z 中心與最低 Y，而不是 FAWE 的一般放置位置 |
| `-e` | 一併複製選區內的實體 |
| `-m <copyMask>` | 只有符合這個額外遮罩的位置才寫入剪貼簿 |

`-x` 與 `-m` 同時使用時，實際複製條件是 `<copyMask>` 且不符合
`<searchMask>`。例如尋找礦石周圍的岩石，但不把礦石本身複製：

```text
//copynear -x -m stone diamond_ore 32
//copynear -xbce #tag[logs] 48
```

同樣地，實際輸入時每個遮罩必須是不含空白的單一參數。以空氣或很寬鬆的
遮罩搜尋大半徑，可能快速碰到 `MAX_CHECKS` 或建立很大的剪貼簿。

## `//autorotatepaste`：依選區方向旋轉貼上

別名：`//arp`

```text
//autorotatepaste [-abenosr] [-m <sourceMask>]
```

基本流程：

1. 建立第一個有方向的選區並執行原生 `//copy`。FaweKit 會記住這次選區從
   主點到第二點的方向。
2. 建立新的有方向選區。新選區主點預設是貼上目的地。
3. 執行 `//arp`。命令會尋找可讓舊方向對齊新方向、轉動次數最少的
   X/Y/Z 軸 90 度旋轉，套到目前剪貼簿 transform 後貼上。

方向只比較各軸的正、負、零，不比較長度。若兩個方向無法用 90 度軸向旋轉
互相對齊，命令會拒絕操作。長方體以 `pos1` 到 `pos2` 決定方向；多邊形及
凸多面體會使用其代表性的最後點，其他選區則使用最大點。

旗標：

| 旗標 | 行為 |
| --- | --- |
| `-a` | 忽略來源剪貼簿中的空氣 |
| `-b` | 貼上生態域 |
| `-e` | 貼上實體 |
| `-n` | 不修改世界，只把目前選區改成旋轉後的長方體邊界 |
| `-o` | 以剪貼簿儲存的原始 origin 座標作為目的地 |
| `-s` | 貼上後把目前選區改成貼上內容的長方體邊界 |
| `-r` | 使用 FAWE 一般放置位置，而不是新選區主點 |
| `-m <sourceMask>` | 只貼上符合來源遮罩的剪貼簿位置 |

`-o` 與 `-r` 互斥。若都不使用，剪貼簿 origin 會對齊目前選區主點。

```text
//copy
//arp -as
//arp -r -abe
//arp -n
```

注意：每次執行都會把新旋轉合成到目前 ClipboardHolder 的既有 transform，
因此連續執行會累積旋轉。要從未旋轉狀態重新計算時，請重新 `//copy` 或重新
載入 schematic。插件啟動後至少要成功建立方向並執行一次 `//copy`，才能使用
`//arp`。

## `//msel`：選取堆疊

```text
//msel <push|pop|combine|delete|clear|list|undo|redo> ...
```

堆疊頂端的正索引是 `1`，往下依序為 `2`、`3`。讀取與刪除時，`-1` 表示
底端、`-2` 表示倒數第二項。堆疊只存在伺服器記憶體中，重啟或重載插件後會
消失；需要持久保存時使用 `//ssel save -m`。

### `push`

```text
//msel push [index]
```

把目前選區的複本放進堆疊。省略索引或使用 `0` 會放到頂端。正索引會插在
該項之前，超過底端時放到最底；負索引會插在由底端倒數之項的後方。

### `pop`

```text
//msel pop [count|all]
```

預設取出頂端 1 項。取出一項時，該項成為目前選區；取出多項或 `all` 時，
這些選區會合併為可供原生 FAWE 編輯命令使用的聯集選區。數量必須介於 `1`
與目前堆疊大小之間。

### `combine`

```text
//msel combine
```

取出整個堆疊並將所有項目合併為目前的聯集選區。空堆疊無法合併；效果等同
`//msel pop all`。

### `delete` 與 `clear`

```text
//msel delete <index>
//msel clear
```

`delete` 移除指定項目，支援正、負索引；`clear` 移除全部堆疊項目。兩者都不
會清除目前啟用的 FAWE 選區。

### `list`

```text
//msel list [-dn] [-p <page>]
```

每頁顯示 10 項，頁碼從 `0` 開始。每一列會顯示從 `1` 開始的索引、選區類型與包圍
座標；點擊該列會執行 `//tpsel -s <index>` 傳送到該選區。`-d` 與 `-n` 是
相容顯示旗標，目前不改變輸出。

### `undo` 與 `redo`

```text
//msel undo
//msel redo
```

最多保存 50 次堆疊變更。這組復原只影響堆疊內容，不是方塊編輯的 `//undo`，
也不會把 `pop` 或 `combine` 所設定的目前選區一併復原。執行新的堆疊變更會
清除 redo 記錄。

範例工作流程：

```text
//msel push
//msel push
//msel list
//msel combine
//msel undo
```

## `//ssel`：儲存選區

```text
//ssel <save|load|list|search|move|delete|clear|formats> ...
```

每位玩家的檔案存於：

```text
plugins/FaweKit/selections/<玩家 UUID>/<name>.sel.yml
```

名稱長度須為 1 到 64 字元，只能包含英文字母、數字、點、底線及連字號；
`.` 與 `..` 不可作為名稱。

### 儲存與載入

```text
//ssel save [-m] <name>
//ssel load <name>
```

- `save` 保存目前選區、世界名稱與格式版本。相同名稱會直接覆寫。
- `-m` 會額外保存目前 `//msel` 堆疊。
- `load` 會在檔案記錄的世界中恢復選區；該世界必須已載入。
- 若檔案含有堆疊，載入時會取代目前堆疊並建立一筆可由 `//msel undo` 復原的
  堆疊變更。若檔案不含堆疊，現有堆疊保持不變。
- 載入另一個世界的選區不會傳送玩家；玩家應先前往該世界再操作它。

支援長方體、多邊形、凸多面體、圓柱體、橢球體，以及由 `//msel` 產生的
聯集選區。

```text
//ssel save castle
//ssel save -m village-plan
//ssel load village-plan
```

### 列出與搜尋

```text
//ssel list [filter]
//ssel search <text>
```

兩者都依名稱做不分大小寫的子字串篩選並按名稱排序。`list` 省略篩選字串時
顯示全部；`search` 必須提供一個不含空白的搜尋參數。結果目前不分頁。

### 重新命名與刪除

```text
//ssel move <oldName> <newName>
//ssel delete <name>
```

`move` 重新命名檔案，來源必須存在且不會覆寫既有目的檔。`delete` 刪除指定
檔案；若檔案不存在，命令仍會回報操作完成。

### 清除目前選區與查看格式

```text
//ssel clear
//ssel formats
```

`clear` 只清除目前啟用的選區，不刪除儲存檔，也不清除 `//msel` 堆疊。
`formats` 顯示目前唯一支援的儲存格式：YAML (`.sel.yml`)。

## `//bmask`：生態域全域遮罩

```text
//bmask <biome>[,<biome>...]
//bmask clear
//bmask
```

第一種寫法會把目前 FAWE LocalSession 的全域遮罩替換成生態域遮罩，後續
編輯只允許落在列出的生態域中。可以省略 `minecraft:` 命名空間，名稱不分
大小寫。逗號前後的聊天參數空白會被移除。

```text
//bmask plains,forest
//bmask minecraft:desert,minecraft:badlands
```

`//bmask clear` 與不帶參數的 `//bmask` 都會把全域遮罩設為空。這會清除目前
任何全域遮罩，不只清除先前由 `//bmask` 建立的遮罩。

## 環境遮罩

FaweKit 把以下遮罩註冊到 FAWE 的 MaskFactory，所以可放在任何接受原生 FAWE
遮罩的命令中。它們沒有獨立權限；實際權限由正在執行的 FAWE 命令決定。

### 無參數遮罩

| 遮罩 | 符合條件 |
| --- | --- |
| `#visible` | 六個正交相鄰位置中，至少一格不是不透明方塊 |
| `#sky` | 該位置上方直到世界最高 Y 都是空氣；玻璃等非空氣方塊也會阻擋 |
| `#transparent` | 目前方塊材質不是不透明材質 |
| `#conductive` | 目前方塊材質不透明，且方塊不是 `minecraft:observer` |
| `#haslight` | 天空光或發射光至少一項大於 `0` |
| `#nolight` | 天空光與發射光都等於 `0` |

### 數值範圍遮罩

```text
#skylight[<level>]
#skylight[<minimum>][<maximum>]
```

下列五種遮罩都支援相同格式，而且至少要提供一組方括號。提供一個值時只
符合該值；提供兩個值時包含上下界。值必須是 `0` 到 `15` 的整數，且最小值
不能大於最大值。

| 遮罩 | 讀取的數值 |
| --- | --- |
| `#skylight` | 該位置的天空光 |
| `#blocklight` | FAWE extent 在該位置回報的發射光 |
| `#light` | 天空光與發射光兩者中的較大值 |
| `#emitslight` | 目前方塊材質本身的發光值 |
| `#opacity` | 該位置的不透明度 |

範例：

```text
//replace #visible stone
//replace #skylight[15] glass
//replace #light[0][3] glowstone
//multireplace #emitslight[1][15] sea_lantern #nolight air
```

## `//help-masks`：遮罩速查

```text
//help-masks
```

顯示一份固定的簡短範例清單，內容包括：

- 單方塊與反向遮罩：`stone`、`!stone`
- 或條件與且條件：`stone,dirt`、`stone dirt`
- `#existing`、`#surface`
- `>stone`、`<stone`
- Minecraft tag：`#tag[mineable/pickaxe]`
- 使用 `//bmask` 依生態域限制編輯

這是速查表，不是 FAWE 所有遮罩的完整列表。權限預設開放給所有玩家，也可
由主控台執行。

## `//help-patterns`：圖樣速查

```text
//help-patterns
```

顯示一份固定的簡短範例清單，內容包括：

- 單方塊與權重隨機：`stone`、`70%stone,30%dirt`
- `#clipboard`、`#existing`
- 複製相容方塊狀態：`^stone`
- 偏移圖樣：`#offset[1][0][0][stone]`
- 依序重複：`#linear[stone,dirt]`

這是速查表，不是 FAWE 所有圖樣的完整列表。權限預設開放給所有玩家，也可
由主控台執行。

## `//echo`：展開方塊名稱萬用字元

```text
//echo <text...>
```

命令逐一檢查以空白分隔的參數，把 Minecraft 方塊名稱中的 glob 展開後顯示：

- `*` 符合零個以上任意字元。
- `?` 符合恰好一個字元。
- 只搜尋 `minecraft` 命名空間；可寫或省略 `minecraft:`。
- glob 資源名稱必須使用小寫英數、底線、點、逗號、`-`、`*` 或 `?`。
- 結果按名稱排序並以逗號連接；沒有符合項目時保留原文字。

```text
//echo //replace mud_brick_* stone
//echo minecraft:*_log
```

輸出會以 `@> ` 開頭。`//echo` 只顯示展開後文字，不會執行它，也不會自動讓
其他命令支援 glob。它可由主控台執行。

## `//shortcut`：玩家捷徑

別名：`//sc`

```text
//sc <new|delete|move|list|search|history|export|name> ...
```

一般捷徑名稱可含英文字母、數字、點、底線及連字號，長度為 1 到 64；名稱
不分大小寫並以小寫儲存。以 `#` 開頭的名稱是文字片段捷徑，規則另見下方。
`new`、`delete`、`move`、`list`、`search`、`history`、`export` 與 `import` 會
固定被解讀為子命令，不應拿來當一般捷徑名稱。

### 建立、覆寫、刪除與改名

```text
//sc new <name> <command-or-text...>
//sc delete <name>
//sc move <oldName> <newName>
```

- `new` 建立捷徑；相同名稱會覆寫。
- `delete` 要求捷徑必須存在。
- `move` 重新命名捷徑。若新名稱已存在，其內容會被覆寫。

```text
//sc new walls //walls ${1:-stone}
//sc move walls buildwalls
//sc delete buildwalls
```

### 執行命令捷徑

```text
//sc <name> [argument...]
```

沒有名為 `execute` 的額外子命令；直接把捷徑名稱放在 `//sc` 後方即可。展開
結果開頭可以有 `/`，也可以省略，最後會以玩家身分交給 Bukkit 執行，因此
仍受目標命令本身的權限限制。

參數展開格式：

| 格式 | 行為 |
| --- | --- |
| `${1}`、`${2}` | 第一、第二個呼叫參數；缺少時變成空字串 |
| `${@}` | 所有呼叫參數，以空白連接 |
| `${1:-default}` | 參數為空時使用 `default` |
| `${1:+alternate}` | 有提供參數時改用 `alternate`，否則為空 |
| `${1:?message}` | 缺少參數時取消並顯示 `message` |

```text
//sc new walls //walls ${1:-stone}
//sc walls
//sc walls deepslate
//sc new required //set ${1:?請提供方塊}
//sc new passthrough //replace ${@}
```

### `#` 文字片段捷徑

```text
//sc new #ground stone,dirt,grass_block
//set #ground
```

以 `#` 開頭的捷徑不能用 `//sc #ground` 當命令執行。FaweKit 會在玩家送出的
其他命令中尋找這個 token 並做文字替換，主要用途是重用 FAWE 遮罩或圖樣。
片段可以引用另一個 `#` 片段，最多遞迴展開 5 層；超過就取消命令。為了能
建立或管理片段，`//shortcut` 與 `//sc` 命令本身不進行這種展開。

### 列表、搜尋與歷史

```text
//sc list [filter]
//sc search [filter]
//sc history [filter]
```

- `list` 與 `search` 行為相同，依名稱或內容做不分大小寫的子字串篩選，按
  名稱排序，目前不分頁。
- 命令歷史保存最近 200 筆未取消命令，最新在前；連續完全相同的命令只記
  一次。
- `history` 最多顯示符合篩選的前 20 筆。

### 匯出

```text
//sc export
```

將捷徑內容寫到：

```text
plugins/FaweKit/shortcuts/exports/<玩家 UUID>.yml
```

匯出檔會覆寫同一玩家的舊匯出，只包含捷徑，不包含命令歷史。`//sc import`
刻意停用，不會從 URL 或檔案自動匯入。

捷徑與歷史平時持久保存在：

```text
plugins/FaweKit/shortcuts/<玩家 UUID>.yml
```

## `//pin` 與 `//unpin`：固定 FAWE 命令位置

```text
//pin
//unpin
```

`//pin` 記住玩家目前的 FAWE 位置。啟用後，FaweKit 會用 FAWE 的
LocationMaskedPlayerWrapper 執行後續原生 `//` 命令，讓命令看到固定的位置，
但玩家本人不會被傳送，原有 FAWE session 與權限也不會被替換。

適合需要移動角色觀察、但希望 `//paste`、筆刷或其他原生命令仍以原地點為
基準的工作流程。`//unpin` 解除固定。

限制：

- 只攔截原生 FAWE 雙斜線命令。
- 不影響一般 `/tp`、其他 Bukkit 命令或玩家實際位置。
- 不影響本手冊中的 FaweKit 自有命令；它們仍看到玩家目前位置。
- 固定狀態只保存在記憶體中，插件或伺服器重啟後會消失。

## 相容語法

下列輸入不是新的編輯引擎。FaweKit 只改寫命令字串，再交給目前安裝的 FAWE
原生命令執行，所以參數、限制與權限都由 FAWE 決定。

| 輸入語法 | 改寫結果 |
| --- | --- |
| `//repeat ...` | `//stack ...` |
| `//unextend ...` | `//contract ...` |
| `//seldraw ...` | `//drawsel ...` |
| `//upload ...` | `//download ...` |
| `//tracemask ...` | `/tracemask ...`，改成單斜線命令 |
| `//sel clear` | `//sel` |
| `//gmask clear` | `//gmask` |
| `//rotate <y> clockwise [其他旋轉值...]` | `//rotate <y> [其他旋轉值...]` |
| `//rotate <y> counterclockwise [其他旋轉值...]` | `//rotate <-y> [其他旋轉值...]` |

`clockwise` 形式的 `<y>` 必須是整數或小數。`counterclockwise` 會把數值變成
負數，第三個參數之後的內容原樣保留。例如：

```text
//rotate 90 clockwise
//rotate 90 counterclockwise 0 0
```

會分別改寫成：

```text
//rotate 90
//rotate -90 0 0
```

`//sel clear` 與 `//gmask clear` 只有在剛好兩個詞時才改寫；附加其他參數會
交回 FAWE 依原輸入處理。

## `//schematic search`：搜尋 schematic

```text
//schematic search [-dfn] [-p <page>] <text...>
```

這是對 FAWE 原生 `//schematic` 命令加入的搜尋形式：

- 從 FAWE 設定的 schematic 根目錄遞迴尋找檔案。
- 若 FAWE 啟用玩家專屬 schematic，根目錄會再加上玩家 UUID。
- 支援 `.schem`、`.schematic` 與 `.mcedit`。
- 先用不分大小寫的字元順序模糊分數排序；無法依序符合時，再以編輯距離
  排序。這是排序而非硬性過濾，所以低相似度檔案仍可能出現在後面頁數。
- 每頁 10 項，頁碼從 `0` 開始。
- 點擊結果會執行原生 `//schematic load <相對路徑>`，載入名稱會移除副檔名。
- `-d`、`-f`、`-n` 是相容顯示旗標，目前不改變輸出。

```text
//schematic search castle
//schematic search -p 1 medieval house
//schematic search -dfn tree
```

這個功能使用 FAWE 原生載入權限 `worldedit.schematic.load`，沒有額外的
`fawekit.*` 權限。必須在 `search` 後提供搜尋文字；只輸入
`//schematic search` 會由 FAWE 原生命令自行處理。

## 權限總表

| 權限 | Bukkit 預設值 | 控制範圍 |
| --- | --- | --- |
| `fawekit.tpsel` | `op` | `//tpsel`、`//seltp` |
| `fawekit.multireplace` | `op` | `//multireplace`、`//multirepl` |
| `fawekit.clipboard` | `op` | 所有 `//clipboard` 子命令 |
| `fawekit.copynear` | `op` | `//copynear` |
| `fawekit.autorotatepaste` | `op` | `//autorotatepaste`、`//arp` |
| `fawekit.msel` | `op` | 所有 `//msel` 子命令 |
| `fawekit.ssel` | `op` | 所有 `//ssel` 子命令 |
| `fawekit.bmask` | `op` | `//bmask` |
| `fawekit.help` | `true` | `//help-masks`、`//help-patterns` |
| `fawekit.echo` | `op` | `//echo` |
| `fawekit.shortcut` | `op` | `//shortcut`、`//sc` |
| `fawekit.pin` | `op` | `//pin`、`//unpin` |
| `worldedit.schematic.load` | 由 FAWE 決定 | `//schematic search` 與載入結果 |

環境遮罩與相容語法沒有獨立的 `fawekit.*` 權限；它們最後仍由使用它們的
FAWE 原生命令檢查權限。上述 `op` 與 `true` 是 `plugin.yml` 的 Bukkit 預設
值，權限插件可以另行覆寫。
