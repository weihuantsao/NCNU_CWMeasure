# Measure App

![Contributors][contributors-shield]
![MIT License][license-shield]

## 開發進度

### 2021/04/07 
- [x] 顯示是否測距前，畫面未先標點。
在觸碰畫布進行觸碰點及兩個量測點尋找的同時會跳出「是否量測」的提示框，此時的底圖上應該要有當前觸碰點及兩個量測點的標記，而非在按下提示框的確認後才顯示。
- [x] 切換原圖/修圖顯示時，座標轉換問題。（參考影片：原圖_修圖切換問題）
例如在使用Canny方式量測結果的畫面，放大確認畫面，此時（畫面是放大到該圖某塊區域的）按下原圖/修圖切換時，畫面會切換成整張的原圖，但標記點依舊會停留在放大畫面時的座標點位。（猜測單純是作為底圖的原圖沒有做到Zoom的部分）。
- [x] 切換二值化方法後，放大縮小的時候，顯示的圖層會是原本方法的圖層。（參考影片：修圖方式切換問題）
例如一開始是選擇Canny方式進行轉換顯示，後續在切換修圖方法後（轉成Binary），點擊畫面測量時會顯示出Binary的圖及其之測量結果，但在此時對畫面做拖曳或是縮放時，圖層會跳回Canny，但再次觸碰測量時依舊會使用Binary。
- [x] 在測量畫面時，按下觸碰點進行測量的前後底圖有更動（但目前不影響測量結果）。（參考影片：圖層跳動問題）->土木老師擔心測量的圖片不是同一張(跳動前跟跳動後的不一樣)，由於04/27開會時提出測量誤差變大問題，需確定是不是因為此處變動導致。
- [ ] HistoryDetail的重新測量(edit)無法正常使用。（瘋狂loading影像處理中）
- [ ] 希望能將資料夾檔案名稱、手機APP顯示名稱統一改名為「CWMeasure_HT01」。

### 2021/04/27 
- [ ] 拔除AR圖層，改用2D標點方式，且額外儲存一張圖。
      (改出兩個版本:(1)保留原AR圖層的黑色方框以及新的2D圖層標點(為了確定圖層的黑色方框及2D標點的位置相同)、(2)拔除AR圖層，僅留下2D標點，後續版本僅需留存第二版本即可)。
- [ ] 上述的最終版本僅需產出一張帶有標點的原圖(在鏡頭畫面的圖層上標點)即可，而後續主要測試使用的APP依舊需要儲存以下三種圖：原圖(不需標點，僅儲存鏡頭畫面不需儲存AR圖層)、Canny圖、Binary圖。
- [x] canny跟Binary的預設罰值以及調整問題。(在CWMesure.xml中，預設Canny罰值(Seekbar)為中間值、(使用切換修圖方式後檢視預設的Binary)Binary小於中間值，在此時調整Binary的罰值(調整至中間值)時，點下切換修圖方式後檢視Canny的值會到最大值。希望更改成預設的罰值皆為中間值，調整的罰值兩邊同步更動。)->無同步更動，但其他皆滿足
- [ ] 待釐清問題(誤差變大)。->目前不需動作，待實驗端(土木系)數據分析出來再看是不是程式問題。
- [ ] 以上04/07到04/27問題，土木老師希望能在4/28產出改良版本。

### 接下來得處理的部分
- [ ] 利用google mediapipe辨識出圖形。(https://github.com/google/mediapipe)
- [ ] 或是google ML Kit。(https://github.com/googlesamples/mlkit)
- [ ] 或是買Calibration board然後利用這個方式修正形變(https://docs.opencv.org/master/dc/dbb/tutorial_py_calibration.html)
- [ ] 使用上述影像辨識方式，辨識出已知圖形並對照片做形變修正後，再使用已知圖形之實際尺寸與畫面所佔的pixel數換算出比例尺，以此比例尺取代AR測距的比例尺應用到測量中。
- [ ] 利用openCV中的findcountour對全圖裂縫進行定義，並將定義出的「所有裂縫」進行測量並儲存其測量點及結果。


## 主要頁面說明



## DataBase Table

圖檔資料db
|  欄位 | 類型  | 說明  | 值|
| ------------ | ------------ | ------------ | ------------ |
|   1|   2| 3  |4 |
|   name|   String|   原圖名稱| NAME|
|   image_type|   String|   修圖方式| IMAGE_TYPE|
|   touchpoint|   point|   最後一次量測之螢幕觸碰點| X|
|   mesurepoint1|   point|   測量點1的座標| X1|
|   mesurepoint2|   point|   測量點2的座標| X2|
|   scale|   double|   最後一次測量方式之比例尺(每1pixel為多少mm，單位mm)| SCALE|
|   result|   double|   最後一次測量之量測結果(單位mm)| RESULT|
|   message|   String|   | MESSAGE|

單一圖片掃描整張圖時儲存點用db -> 用json
|  欄位 | 類型  | 說明  | 值|
| ------------ | ------------ | ------------ | ------------ |
|   1|   2| 3  |4 |
|   name|   String|   原圖名稱| NAME|
|   mesurepoint1|   point|   測量點1的座標| X1|
|   mesurepoint2|   point|   測量點2的座標| X2|
|   scale|   double|   比例尺(每1pixel為多少mm，單位mm)| SCALE|
|   result|   double|   量測結果(單位mm)| RESULT|

## 使用第三方


<!-- MARKDOWN LINKS & IMAGES -->
<!-- https://www.markdownguide.org/basic-syntax/#reference-style-links -->
[contributors-shield]: https://img.shields.io/badge/Contributors-2-green?style=for-the-badge
[issues-shield]: https://img.shields.io/github/issues/othneildrew/Best-README-Template.svg?style=for-the-badge
[license-shield]: https://img.shields.io/badge/license-MIT-blue?style=for-the-badge
 
