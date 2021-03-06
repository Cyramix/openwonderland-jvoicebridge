/*
 * Copyright 2007 Sun Microsystems, Inc.
 *
 * This file is part of jVoiceBridge.
 *
 * jVoiceBridge is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License version 2 as 
 * published by the Free Software Foundation and distributed hereunder 
 * to you.
 *
 * jVoiceBridge is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Sun designates this particular file as subject to the "Classpath"
 * exception as provided by Sun in the License file that accompanied this 
 * code. 
 */

#include <stdio.h>
#include <stdlib.h>
#include <sys/audioio.h>
#include <sys/time.h>
#include <errno.h>
#include <fcntl.h>

static const char *sun_audio_device = "/dev/audio";

int audio_fd;

/**
 * Opens the audio device and sets the global audio_fd variable.
 */
void open_audio() {
    audio_info_t ainfo;
    char *audio_device;

    if ((audio_fd = open(sun_audio_device, O_RDWR)) < 0) {
        /* the device might be a Sun Ray, so get the AUDIODEV env var
         */
        audio_device = getenv("AUDIODEV");
	
	if (audio_device != NULL) {
   	    if ((audio_fd = open(audio_device, O_RDWR)) < 0) {
	        printf("sun_audio: failed to open audio device %s: %s\n",
                       audio_device, strerror(errno));
	    }
	} else {
	    printf("sun_audio: failed to open audio device %s: %s\n",
                   sun_audio_device, strerror(errno));
	}
    }
    
    if (audio_fd < 0) {
        exit(0);
    }
    
    ioctl(audio_fd, AUDIO_GETINFO, &ainfo);

    ainfo.play.encoding = ainfo.record.encoding = AUDIO_ENCODING_ULAW;
    ainfo.play.precision = ainfo.record.precision = 8;
    ainfo.play.channels = ainfo.record.channels = 1;
    ainfo.play.sample_rate = ainfo.record.sample_rate = 8000;
    ainfo.play.buffer_size = ainfo.record.buffer_size = 160;
    
    ainfo.record.gain = 180;
    ainfo.record.port = AUDIO_MICROPHONE;
    ainfo.record.samples = 0;
    ainfo.record.pause = 0;
    ainfo.record.error = 0;
    
    if (ioctl(audio_fd, AUDIO_SETINFO, &ainfo) == -1) {
	printf("sun_audio: failed to set audio params: %s\n",
               strerror(errno));
	close(audio_fd);
        exit(0);
    }

    if (ainfo.play.encoding == AUDIO_ENCODING_ULAW)
        printf("sun_audio: encoding is AUDIO_ENCODING_ULAW\n");
    else {
        printf("sun_audio: audio format (%d bit/encoding #%d)\n",
               ainfo.play.precision, ainfo.play.encoding);
    }
    printf("sun_audio: play.buffer_size is %d\n", ainfo.play.buffer_size);
    printf("sun_audio: record.buffer_size is %d\n", ainfo.record.buffer_size);
    
}

int flush_audio() {
    return ioctl(audio_fd, AUDIO_DRAIN, 0);
}

int main(int argc, char **argv) {
    unsigned short buf[300];
    int k;
    open_audio();
    while (1) {
	hrtime_t startTime = gethrtime();
	hrtime_t endTime;

        k = read(audio_fd, buf, sizeof(buf));
        /*printf("read %d\n", k);*/

	endTime = gethrtime();

	/*printf("time = %lld millis\n", (endTime - startTime) / 1000);*/
	
        k = write(audio_fd, buf, k);
    }
}
