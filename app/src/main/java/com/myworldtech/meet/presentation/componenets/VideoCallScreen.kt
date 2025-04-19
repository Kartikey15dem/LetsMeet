package com.myworldtech.meet.presentation.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.myworldtech.meet.presentation.model.Participant
import com.myworldtech.meet.presentation.viewmodels.VideoCallViewModel
import com.myworldtech.meet.ui.theme.MeetTheme

@Composable
fun VideoCallScreen(
    modifier: Modifier = Modifier,
    viewModel: VideoCallViewModel
//    participants : List<Int> = listOf(1, 2,3,4,5,6,7,8)
)
{
    val participants = viewModel.participants.value
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

    if(participants.size <= 4) {
        MyGridCell(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 26.dp, bottom = 26.dp),
            height = 170,
            width = 100,
            participant = participants[0]
        )
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (i in 1..participants.size - 1) {
                ParticipantGridCell(modifier.weight(1f),participants[i])
            }
        }
    }
    if(participants.size == 5) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            ParticipantGridCell(modifier.weight(1f),participants[participants.size - 1])
            for (i in 2 downTo 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    ParticipantGridCell(modifier.weight(1f),participants[2*i - 1])
                    ParticipantGridCell(modifier.weight(1f),participants[i - 1])
                }
            }
        }
    }
    if(participants.size == 7) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ){
                    for(i in 4..6) {
                        ParticipantGridCell(modifier.weight(1f),participants[i])
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ){
                    for(i in 3 downTo 0) {
                        ParticipantGridCell(modifier.weight(1f),participants[i])
                    }
                }
        }
    }
    if(participants.size == 6 || participants.size == 8){
      Column(
          modifier = Modifier.fillMaxHeight(),
          verticalArrangement = Arrangement.SpaceEvenly
      ) {
            for (i in participants.size/2  downTo 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    ParticipantGridCell(modifier.weight(1f),participants[2*i - 1])
                    ParticipantGridCell(modifier.weight(1f),participants[i -1])
                }
            }
      }
    }
    }
}

@Composable
fun MyGridCell(
    modifier: Modifier,
   height: Int,
    width: Int,
    participant: Participant,
) {
    key(participant) {
        Box(
            modifier = modifier.width(width.dp)
                .height(height.dp)
        ) {
            Text("MyGridCell")
        }
    }
}
@Preview
@Composable
fun Prev(modifier: Modifier = Modifier) {
    MeetTheme {
//        VideoCallScreen()
    }
}