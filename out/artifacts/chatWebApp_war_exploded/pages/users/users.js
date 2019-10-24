var chatVersion = 0;
var refreshRate = 2000; //milli seconds
var USER_LIST_URL = buildUrlWithContextPath("userslist");
var MESSAGE_LIST_URL = buildUrlWithContextPath("messages");
var GET_ACTIVE_USER = 1;
var GET_USERLIST = 2;
var CLONE = 3;

function refreshUsersList(users) {
    $.each(users || [], function(index, username) {
        var hasOption = $('#usersList option[value="' + username + '"]');
        if (hasOption.length == 0){
            var option = $('<option>', {value: username, text: username});
            option.appendTo($("#usersList"));
        }
    });
    if(!$('#usersList').val()[0])
        $('#usersList option[value="' + document.getElementById('activeUser').innerHTML + '"]').prop({selected: true});
}

$(function() { // onload...do
    var data = "reqType=" + GET_ACTIVE_USER;
    $.ajax({
        method: 'GET',
        url: "users",
        data: data,
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            document.getElementById('activeUser').innerHTML = r;
        }
    });

    $('#usersList').change(onUserListChanged);
});

function onUserListChanged(){
    var username = $('#usersList').val()[0];
    getRepositoryByName(username);
}

//entries = the added chat strings represented as a single string
function appendToChatArea(entries) {
//    $("#chatarea").children(".success").removeClass("success");
    
    // add the relevant entries
    $.each(entries || [], appendChatEntry);
    
    // handle the scroller to auto scroll to the end of the chat area
    var scroller = $("#chatarea");
    var height = scroller[0].scrollHeight - $(scroller).height();
    $(scroller).stop().animate({ scrollTop: height }, "slow");
}

function appendChatEntry(index, entry){
    var entryElement = createChatEntry(entry);
    $("#chatarea").append(entryElement).append("<br>");
}

function createChatEntry (entry){
    entry.chatString = entry.chatString.replace (":)", "<img class='smiley-image' src='../../common/images/smiley.png'/>");
    return $("<span class=\"success\">").append(entry.username + "> " + entry.chatString);
}

function ajaxUsersList(){
    var data = "reqType=" + GET_USERLIST;
    $.ajax({
        url: USER_LIST_URL,
        data: data,
        success: function(users) {
            refreshUsersList(users);
        }
    });
}

//call the client.server and get the chat version
//we also send it the current chat version so in case there was a change
//in the chat content, we will get the new string as well
function ajaxChatContent() {
    $.ajax({
        url: "messages",
        data: "msgVersion=" + chatVersion,
        success: function(data) {
            /*
             data will arrive in the next form:
             {
                "entries": [
                    {
                        "chatString":"Hi",
                        "username":"bbb",
                        "time":1485548397514
                    },
                    {
                        "chatString":"Hello",
                        "username":"bbb",
                        "time":1485548397514
                    }
                ],
                "version":1
             }
             */
            console.log("Server chat version: " + data.version + ", Current chat version: " + chatVersion);
            if (data.version !== chatVersion) {
                chatVersion = data.version;
                appendToChatArea(data.entries);
            }
            triggerAjaxChatContent();
        },
        error: function(error) {
            triggerAjaxChatContent();
        }
    });
}

$(function() { // onload...do
    //add a function to the submit event
    $("#msgform").submit(function() {
        $.ajax({
            data: $(this).serialize(),
            url: "getMessages",
            timeout: 2000,
            error: function() {
                console.error("Failed to submit");
            },
            success: function(r) {
                //do not add the user string to the chat area
                //since it's going to be retrieved from the client.server
                //$("#result h1").text(r);
            }
        });

        $("#messages").val("");
        // by default - we'll always return false so it doesn't redirect the user.
        return false;
    });
});

function triggerAjaxChatContent() {
    setTimeout(ajaxChatContent, refreshRate);
}

//activate the timer calls after the page is loaded
$(function() {
    //The users list is refreshed automatically every second
    setInterval(ajaxUsersList, refreshRate);
    
    //The chat content is refreshed only once (using a timeout) but
    //on each call it triggers another execution of itself later (1 second later)
    triggerAjaxChatContent();
});

$(function() { // onload...do
    getRepositories();
});

function getRepositories(){
    $("#activeUser").val("")
    $("#uploadForm").submit(function() {
        var file = this[0].files[0];
        var formData = new FormData();
        formData.append("fake-key-1", file);
        $.ajax({
            method:'POST',
            data: formData,
            url: this.action,
            processData: false, // Don't process the files
            contentType: false, // Set content type to false as jQuery will tell the server its a query string request
            timeout: 4000,
            error: function(e) {
                alert(e.responseText);
            },
            success: function(r) {
                updateRepositoriesTable(r);
            }
        });
        // return value of the submit operation
        // by default - we'll always return false so it doesn't redirect the user.
        return false;
    });
}

function getRepositoryByName(username) {
    $.ajax({
        method: 'PUT',
        data: username,
        url: "upload",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            $('#repositoriesTable tbody').empty();
            for (var i = 0 ; i < r.length; i++) {
                updateRepositoriesTable(JSON.parse(r[i]), username);
            }
        }
    });
}

function insertForkedRepositoryToList(){
    var username = document.getElementById('activeUser').innerHTML;
    $.ajax({
        method: 'GET',
        data: username,
        url: "upload",
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            $('#repositoriesTable tbody').empty();
            for (var i = 0 ; i < r.length; i++) {
                updateRepositoriesTable(JSON.parse(r[i]), username);
            }
        }
    });
}

function fork(repoOwner, repoName){
    var req = "reqType=" + CLONE + "&owner=" + repoOwner + "&repoName=" + repoName;
    $.ajax({
        method: 'GET',
        processData: false, // Don't process the files
        contentType: false, // Set content type to false as jQuery will tell the server its a query string request
        timeout: 4000,
        data: req,
        url: "users",
        error: function (e) {
            alert(e.responseText);
        },
        success: function (r) {
            insertForkedRepositoryToList();
            $.ajax({
                method: 'POST',
                data: repoName,
                url: "repo",
                processData: false, // Don't process the files
                contentType: false, // Set content type to false as jQuery will tell the server its a query string request
                timeout: 4000,
                error: function (e) {
                    alert(e.responseText);
                },
                success: function (r) {
                    window.location = '../repository/repository.html'
                }
            });
        }
    });
}

function updateRepositoriesTable(r){
    var owner = $('#usersList').val()[0];
    var loggedInUser = document.getElementById('activeUser').innerHTML;
    var htmlString = '<tr class="child" style=\"cursor:pointer\"><td>'
        + r["Name"] + '</td><td>'
        + r["ActiveBranch"] + '</td><td>'
        + r["BranchesAmount"] + '</td><td>'
        + r["CommitDate"] + '</td><td>'
        + r["CommitMessage"] + '</td><td>'
        + r["Forked"] + '</td></tr>';
    $('#repositoriesTable tbody').append(htmlString);
    var rowElement = $.parseHTML(htmlString)
    var table = document.getElementById("repositoriesTable");
    if (table != null) {
        for (var i = 0; i < table.rows.length; i++) {
            for (var j = 0; j < table.rows[i].cells.length; j++)
            if (loggedInUser == owner) { // logged in user
                    table.rows[i].onclick = function (e) {
                        var repoName = e.currentTarget.firstChild.innerText;
                        $.ajax({
                            method: 'POST',
                            data: repoName,
                            url: "repo",
                            processData: false, // Don't process the files
                            contentType: false, // Set content type to false as jQuery will tell the server its a query string request
                            timeout: 4000,
                            error: function (e) {
                                alert(e.responseText);
                            },
                            success: function (r) {
                                window.location = '../repository/repository.html'
                            }
                        });
                    }
                }
            else{
                table.rows[i].onclick = function (ev) {
                    var repoName = ev.currentTarget.firstChild.innerText;
                    var answer = confirm("You cannot open this repository since it's owned by another user. " +
                        "Would you like to fork it?");
                    if(answer){
                        fork(owner, repoName);
                    }
                }
            }
        }
    }
}